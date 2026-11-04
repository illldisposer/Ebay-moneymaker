package com.example.data.repository

import com.example.data.api.AliExpressClient
import com.example.data.api.AliProduct
import com.example.data.api.EbayBrowseClient
import com.example.data.api.EbayItem
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.SystemLogEntity
import com.example.data.matching.ImageFingerprint
import com.example.data.matching.ProductMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

data class ScanResult(
    val productsChecked: Int,
    val autoMatches: Int,
    val reviewMatches: Int,
    val discarded: Int,
    val updated: Int,
    val errors: Int,
    val message: String
)

class EbayBotRepository(
    private val database: AppDatabase
) {
    private val matchDao = database.matchDao()
    private val logDao = database.systemLogDao()
    private val settingDao = database.botSettingDao()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val ebay = EbayBrowseClient(http)
    private val ali = AliExpressClient(http)
    private val fingerprint = ImageFingerprint(http)

    val scannerMatches: Flow<List<MatchEntity>> = matchDao.getScannerMatches()
    val reviewQueue: Flow<List<MatchEntity>> = matchDao.getReviewQueue()
    val reviewCount: Flow<Int> = matchDao.getReviewCount()
    val allLogs: Flow<List<SystemLogEntity>> = logDao.getAllLogs()
    val settings: Flow<BotSettingEntity?> = settingDao.getSettings()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (settingDao.getSettingsOnce() == null) {
                settingDao.insertOrUpdate(BotSettingEntity())
                log("SCAN", "Scanner bereit", "Trage in den Einstellungen deine eBay- und AliExpress-Schlüssel ein und starte den ersten Scan.")
            }
        }
    }

    private suspend fun log(type: String, title: String, details: String, relatedId: String? = null, isError: Boolean = false) {
        logDao.insertLog(
            SystemLogEntity(
                eventType = type,
                title = title,
                details = details,
                relatedItemId = relatedId,
                isError = isError
            )
        )
    }

    private data class CandidateScore(
        val item: EbayItem,
        val confidence: Int,
        val method: String,
        val reason: String
    )

    /**
     * Konfidenz für ein Kandidaten-Paar – komplett lokal: Titelvergleich + Bild-Fingerabdruck (dHash).
     * Fast identische Produktbilder (≤ 6 von 64 Bit Abweichung) gelten mit passendem Titel als sicher.
     */
    private fun scoreCandidate(product: AliProduct, aliHash: Long?, item: EbayItem, textScore: Int): CandidateScore {
        val ebayHash = if (aliHash != null) fingerprint.hashOf(item.imageUrl) else null
        if (aliHash == null || ebayHash == null) {
            return CandidateScore(
                item,
                minOf(textScore, 70),
                "Titelvergleich",
                "Titel-Ähnlichkeit $textScore % (Bild nicht ladbar)"
            )
        }
        val distance = ImageFingerprint.hammingDistance(aliHash, ebayHash)
        val imageScore = ImageFingerprint.imageScore(aliHash, ebayHash)
        return if (distance <= 6) {
            val confidence = if (textScore >= 20) 88 + minOf(11, textScore / 9) else 78
            CandidateScore(
                item,
                confidence,
                "Bild-Fingerabdruck",
                "Nahezu identisches Produktbild ($distance/64 Bit Abweichung) · Titel-Ähnlichkeit $textScore %"
            )
        } else {
            CandidateScore(
                item,
                minOf(80, (0.5 * textScore + 0.5 * imageScore).roundToInt()),
                "Bild + Titel",
                "Bild-Ähnlichkeit $imageScore % · Titel-Ähnlichkeit $textScore %"
            )
        }
    }

    /**
     * Scan: AliExpress-Produkte je Suchbegriff holen, passende eBay-Angebote suchen,
     * lokal zuordnen (EAN → Bild-Fingerabdruck + Titel → Titel) und als AUTO / REVIEW speichern.
     * AliExpress-Daten verlassen dabei das Gerät nicht in Richtung Dritter.
     * Bis zu 3 Produkte werden parallel verarbeitet.
     */
    suspend fun runScan(onProgress: (String) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val s = settingDao.getSettingsOnce() ?: BotSettingEntity()
        if (!s.hasEbayKeys()) throw IllegalStateException("eBay Client ID / Client Secret fehlen – bitte in den Einstellungen eintragen.")
        if (!s.hasAliKeys()) throw IllegalStateException("AliExpress App Key / App Secret / Tracking-ID fehlen – bitte in den Einstellungen eintragen.")
        val keywords = s.keywordList()
        if (keywords.isEmpty()) throw IllegalStateException("Keine Suchbegriffe hinterlegt.")

        fingerprint.clearCache()
        val checked = AtomicInteger(0)
        val autoCount = AtomicInteger(0)
        val reviewCount = AtomicInteger(0)
        val discarded = AtomicInteger(0)
        val updated = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val consecutiveErrors = AtomicInteger(0)
        val parallel = Semaphore(3)

        log("SCAN", "Scan gestartet", "Suchbegriffe: ${keywords.joinToString(", ")} · ${s.productsPerKeyword} Produkte je Begriff · Auto-Schwelle ${s.autoAcceptConfidence} %")

        for (keyword in keywords) {
            coroutineContext.ensureActive()
            onProgress("AliExpress: „$keyword“ …")

            val products: List<AliProduct> = try {
                ali.searchProducts(s, keyword, s.productsPerKeyword).also { consecutiveErrors.set(0) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errors.incrementAndGet()
                log("ERROR", "AliExpress-Suche fehlgeschlagen", "„$keyword“: ${e.message}", isError = true)
                if (consecutiveErrors.incrementAndGet() >= 5) throw IllegalStateException("Scan abgebrochen nach 5 Fehlern in Folge. Letzter: ${e.message}")
                continue
            }

            val done = AtomicInteger(0)
            coroutineScope {
                products.map { product ->
                    async {
                        parallel.withPermit {
                            coroutineContext.ensureActive()
                            checked.incrementAndGet()
                            try {
                                when (processProduct(s, keyword, product)) {
                                    ProductOutcome.AUTO -> autoCount.incrementAndGet()
                                    ProductOutcome.REVIEW -> reviewCount.incrementAndGet()
                                    ProductOutcome.UPDATED -> updated.incrementAndGet()
                                    ProductOutcome.DISCARDED -> discarded.incrementAndGet()
                                }
                                consecutiveErrors.set(0)
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                errors.incrementAndGet()
                                log("ERROR", "Fehler bei Produkt", "${product.title.take(60)}: ${e.message}", relatedId = product.productId, isError = true)
                                if (consecutiveErrors.incrementAndGet() >= 5) throw IllegalStateException("Scan abgebrochen nach 5 Fehlern in Folge. Letzter: ${e.message}")
                            }
                            onProgress("„$keyword“ ${done.incrementAndGet()}/${products.size}: ${product.title.take(40)}")
                        }
                    }
                }.awaitAll()
            }
        }

        settingDao.insertOrUpdate((settingDao.getSettingsOnce() ?: s).copy(lastScanRun = System.currentTimeMillis()))

        val message = "${checked.get()} geprüft · ${autoCount.get()} sicher · ${reviewCount.get()} zum Prüfen · ${updated.get()} aktualisiert · ${discarded.get()} verworfen" +
            if (errors.get() > 0) " · ${errors.get()} Fehler" else ""
        log("MATCH", "Scan beendet", message, isError = false)
        ScanResult(checked.get(), autoCount.get(), reviewCount.get(), discarded.get(), updated.get(), errors.get(), message)
    }

    private enum class ProductOutcome { AUTO, REVIEW, UPDATED, DISCARDED }

    private suspend fun processProduct(s: BotSettingEntity, keyword: String, product: AliProduct): ProductOutcome {
        // 1) eBay-Kandidaten: exakt per EAN, sonst per Suchbegriff (mit kürzerem Fallback)
        val gtin = ProductMatcher.extractGtin(product.title)
        var candidates: List<EbayItem> = emptyList()
        var viaGtin = false
        if (gtin != null) {
            candidates = ebay.search(s, gtin = gtin, limit = s.ebayCandidatesPerProduct)
            viaGtin = candidates.isNotEmpty()
        }
        if (candidates.isEmpty()) {
            val query = ProductMatcher.buildSearchQuery(product.title, 7)
            if (query.isNotBlank()) candidates = ebay.search(s, query = query, limit = s.ebayCandidatesPerProduct)
            if (candidates.isEmpty()) {
                val shortQuery = ProductMatcher.buildSearchQuery(product.title, 4)
                if (shortQuery.isNotBlank() && shortQuery != query) {
                    candidates = ebay.search(s, query = shortQuery, limit = s.ebayCandidatesPerProduct)
                }
            }
        }
        if (candidates.isEmpty()) return ProductOutcome.DISCARDED

        // 2) Bekannte Paare: Entscheidung respektieren – offenes/behaltenes Paar nur aktualisieren, aussortierte ignorieren
        val open = mutableListOf<EbayItem>()
        for (item in candidates) {
            val existing = matchDao.getByPairKey(pairKeyOf(product, item))
            when {
                existing == null -> open.add(item)
                existing.status == "REJECTED" -> Unit
                else -> {
                    matchDao.update(
                        existing.copy(
                            aliPrice = product.price,
                            aliOriginalPrice = product.originalPrice,
                            aliAffiliateUrl = product.affiliateUrl.ifBlank { existing.aliAffiliateUrl },
                            aliSalesVolume = product.salesVolume,
                            ebayPrice = item.price,
                            ebayShipping = item.shipping,
                            ebayCandidateCount = candidates.size,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    return ProductOutcome.UPDATED
                }
            }
        }
        if (open.isEmpty()) return ProductOutcome.DISCARDED

        // 3) Bewerten: EAN-Treffer sofort sicher, sonst Top-3 nach Titel lokal per Bild prüfen
        val best: CandidateScore = if (viaGtin) {
            CandidateScore(open.first(), 99, "EAN", "Identische EAN $gtin")
        } else {
            val byText = open.map { it to ProductMatcher.titleSimilarity(product.title, it.title) }
                .sortedByDescending { it.second }
                .take(3)
            val aliHash = fingerprint.hashOf(product.imageUrl)
            byText.map { (item, text) -> scoreCandidate(product, aliHash, item, text) }
                .maxByOrNull { it.confidence } ?: return ProductOutcome.DISCARDED
        }

        if (best.confidence < s.minConfidence) return ProductOutcome.DISCARDED
        val status = if (best.confidence >= s.autoAcceptConfidence) "AUTO" else "REVIEW"
        val item = best.item

        val inserted = matchDao.insert(
            MatchEntity(
                pairKey = pairKeyOf(product, item),
                keyword = keyword,
                aliProductId = product.productId,
                aliTitle = product.title,
                aliPrice = product.price,
                aliOriginalPrice = product.originalPrice,
                aliCurrency = product.currency,
                aliImageUrl = product.imageUrl,
                aliProductUrl = product.productUrl,
                aliAffiliateUrl = product.affiliateUrl,
                aliSalesVolume = product.salesVolume,
                aliCommissionRate = product.commissionRate,
                aliCategory = product.category,
                ebayItemId = item.itemId,
                ebayTitle = item.title,
                ebayPrice = item.price,
                ebayShipping = item.shipping,
                ebayCurrency = item.currency,
                ebayImageUrl = item.imageUrl,
                ebayItemUrl = item.itemUrl,
                ebayCondition = item.condition,
                ebayCandidateCount = candidates.size,
                matchMethod = best.method,
                confidence = best.confidence,
                matchReason = best.reason,
                status = status
            )
        )
        if (inserted <= 0) return ProductOutcome.UPDATED
        return if (status == "AUTO") ProductOutcome.AUTO else ProductOutcome.REVIEW
    }

    private fun pairKeyOf(product: AliProduct, item: EbayItem): String = "ali:${product.productId}|ebay:${item.itemId}"

    /** Entscheidung aus "Prüfen" (oder "Falsch zugeordnet" im Scanner). */
    suspend fun decideMatch(id: Long, keep: Boolean): String? = withContext(Dispatchers.IO) {
        val match = matchDao.getById(id) ?: return@withContext null
        val now = System.currentTimeMillis()
        matchDao.updateStatus(id, if (keep) "KEPT" else "REJECTED", now)
        log(
            "REVIEW",
            if (keep) "Treffer behalten" else "Treffer aussortiert",
            "${match.aliTitle.take(60)} ↔ ${match.ebayTitle.take(60)} (Sicherheit ${match.confidence} %)",
            relatedId = match.pairKey
        )
        adjustThresholdIfEnabled()
    }

    /**
     * Lernt die Auto-Schwelle aus deinen Entscheidungen: kleinste Schwelle (50-95 %),
     * bei der mindestens 90 % der Treffer darüber von dir behalten wurden.
     * Gibt eine Meldung zurück, wenn sich die Schwelle geändert hat.
     */
    private suspend fun adjustThresholdIfEnabled(): String? {
        val s = settingDao.getSettingsOnce() ?: return null
        if (!s.autoAdjustThreshold) return null
        val decided = matchDao.getDecidedMatches()
        if (decided.size < 8) return null

        var newThreshold = 95
        for (t in 50..95 step 5) {
            val above = decided.filter { it.confidence >= t }
            if (above.size < 3) continue
            val keptRatio = above.count { it.status == "KEPT" }.toDouble() / above.size
            if (keptRatio >= 0.9) {
                newThreshold = t
                break
            }
        }
        newThreshold = newThreshold.coerceAtLeast(s.minConfidence + 5)
        if (newThreshold == s.autoAcceptConfidence) return null

        settingDao.insertOrUpdate(s.copy(autoAcceptConfidence = newThreshold))
        val now = System.currentTimeMillis()
        val promoted = matchDao.promoteReviewToAuto(newThreshold, now)
        val demoted = matchDao.demoteAutoToReview(newThreshold, now)
        val msg = "Auto-Schwelle ${s.autoAcceptConfidence} % → $newThreshold % (aus ${decided.size} Entscheidungen). $promoted nach Scanner, $demoted nach Prüfen verschoben."
        log("THRESHOLD", "Schwelle angepasst", msg)
        return msg
    }

    suspend fun clearUndecided(): Int = withContext(Dispatchers.IO) {
        val n = matchDao.clearUndecided()
        log("SCAN", "Ergebnisse geleert", "$n offene Treffer entfernt. Deine Entscheidungen (Behalten/Raus) bleiben gespeichert.")
        n
    }

    suspend fun testConnections(): String = withContext(Dispatchers.IO) {
        val s = settingDao.getSettingsOnce() ?: BotSettingEntity()
        val lines = mutableListOf<String>()
        var anyError = false

        lines += try {
            ebay.testConnection(s)
        } catch (e: Exception) {
            anyError = true
            "eBay: ${e.message}"
        }
        lines += try {
            ali.testConnection(s)
        } catch (e: Exception) {
            anyError = true
            "AliExpress: ${e.message}"
        }
        lines += "Zuordnung: lokal auf dem Gerät (Titelvergleich + Bild-Fingerabdruck), keine Weitergabe an Dritte."

        val text = lines.joinToString("\n")
        log("CONNECTION", "Verbindungstest", text, isError = anyError)
        text
    }

    suspend fun updateSettings(settings: BotSettingEntity) = withContext(Dispatchers.IO) {
        settingDao.insertOrUpdate(settings)
        val now = System.currentTimeMillis()
        // Neue Auto-Schwelle sofort auf offene Treffer anwenden
        matchDao.promoteReviewToAuto(settings.autoAcceptConfidence, now)
        matchDao.demoteAutoToReview(settings.autoAcceptConfidence, now)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAllLogs()
    }
}
