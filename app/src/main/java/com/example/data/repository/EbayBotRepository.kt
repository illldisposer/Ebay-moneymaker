package com.example.data.repository

import com.example.data.api.AliExpressClient
import com.example.data.api.AliProduct
import com.example.data.api.EbayBrowseClient
import com.example.data.api.EbayItem
import com.example.data.gemini.GeminiApiException
import com.example.data.gemini.GeminiResonanceService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.SystemLogEntity
import com.example.data.matching.ProductMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

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
    private val database: AppDatabase,
    private val geminiService: GeminiResonanceService = GeminiResonanceService()
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

    /**
     * Scan: AliExpress-Produkte je Suchbegriff holen, passendes eBay-Angebot suchen,
     * zuordnen (EAN → KI-Bildvergleich → Titelvergleich) und als AUTO / REVIEW speichern.
     */
    suspend fun runScan(onProgress: (String) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val s = settingDao.getSettingsOnce() ?: BotSettingEntity()
        if (!s.hasEbayKeys()) throw IllegalStateException("eBay Client ID / Client Secret fehlen – bitte in den Einstellungen eintragen.")
        if (!s.hasAliKeys()) throw IllegalStateException("AliExpress App Key / App Secret / Tracking-ID fehlen – bitte in den Einstellungen eintragen.")
        val keywords = s.keywordList()
        if (keywords.isEmpty()) throw IllegalStateException("Keine Suchbegriffe hinterlegt.")

        val geminiKey = geminiService.resolveApiKey(s.geminiApiKey)
        var geminiDisabledReason: String? = if (geminiKey == null) "kein Gemini-Schlüssel" else null

        var checked = 0
        var autoCount = 0
        var reviewCount = 0
        var discarded = 0
        var updated = 0
        var errors = 0
        var consecutiveErrors = 0

        log("SCAN", "Scan gestartet", "Suchbegriffe: ${keywords.joinToString(", ")} · ${s.productsPerKeyword} Produkte je Begriff · Auto-Schwelle ${s.autoAcceptConfidence} %")

        for (keyword in keywords) {
            coroutineContext.ensureActive()
            onProgress("AliExpress: „$keyword“ …")

            val products: List<AliProduct> = try {
                ali.searchProducts(s, keyword, s.productsPerKeyword).also { consecutiveErrors = 0 }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errors++
                consecutiveErrors++
                log("ERROR", "AliExpress-Suche fehlgeschlagen", "„$keyword“: ${e.message}", isError = true)
                if (consecutiveErrors >= 5) throw IllegalStateException("Scan abgebrochen nach 5 Fehlern in Folge. Letzter: ${e.message}")
                continue
            }

            products.forEachIndexed { index, product ->
                coroutineContext.ensureActive()
                checked++
                onProgress("„$keyword“ ${index + 1}/${products.size}: ${product.title.take(40)}")

                try {
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
                    consecutiveErrors = 0
                    if (candidates.isEmpty()) {
                        discarded++
                        return@forEachIndexed
                    }

                    // 2) Besten Kandidaten per Titel-Ähnlichkeit wählen
                    val scored = candidates.map { it to ProductMatcher.titleSimilarity(product.title, it.title) }
                        .sortedByDescending { it.second }
                    val (best, textScore) = scored.first()
                    val pairKey = "ali:${product.productId}|ebay:${best.itemId}"

                    // 3) Bekanntes Paar: Entscheidung respektieren, nur Preise aktualisieren
                    val existing = matchDao.getByPairKey(pairKey)
                    if (existing != null) {
                        if (existing.status != "REJECTED") {
                            matchDao.update(
                                existing.copy(
                                    aliPrice = product.price,
                                    aliOriginalPrice = product.originalPrice,
                                    aliAffiliateUrl = product.affiliateUrl.ifBlank { existing.aliAffiliateUrl },
                                    aliSalesVolume = product.salesVolume,
                                    ebayPrice = best.price,
                                    ebayShipping = best.shipping,
                                    ebayCandidateCount = candidates.size,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            updated++
                        }
                        return@forEachIndexed
                    }

                    // 4) Konfidenz bestimmen
                    var method: String
                    var confidence: Int
                    var reason: String
                    if (viaGtin) {
                        method = "EAN"
                        confidence = 99
                        reason = "Identische EAN $gtin"
                    } else if (textScore < 15) {
                        discarded++
                        return@forEachIndexed
                    } else {
                        val ai = if (geminiDisabledReason == null) {
                            try {
                                geminiService.compareProducts(
                                    apiKey = geminiKey,
                                    model = s.geminiModel,
                                    aliTitle = product.title,
                                    aliImageUrl = product.imageUrl,
                                    ebayTitle = best.title,
                                    ebayImageUrl = best.imageUrl
                                )
                            } catch (e: GeminiApiException) {
                                geminiDisabledReason = e.message
                                log("ERROR", "KI-Bildvergleich abgeschaltet", "${e.message} – restlicher Scan nur mit Titelvergleich.", isError = true)
                                null
                            }
                        } else null

                        if (ai != null) {
                            method = "KI-Bildvergleich"
                            confidence = ai.confidence
                            reason = "${ai.reason} · Titel-Ähnlichkeit $textScore %"
                        } else {
                            // Ohne KI nie automatisch "sicher": max. 70 % → landet in "Prüfen"
                            method = "Titelvergleich"
                            confidence = minOf(textScore, 70)
                            reason = "Titel-Ähnlichkeit $textScore %" + (geminiDisabledReason?.let { " (ohne KI: $it)" } ?: "")
                        }
                    }

                    if (confidence < s.minConfidence) {
                        discarded++
                        return@forEachIndexed
                    }
                    val status = if (confidence >= s.autoAcceptConfidence) "AUTO" else "REVIEW"

                    val inserted = matchDao.insert(
                        MatchEntity(
                            pairKey = pairKey,
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
                            ebayItemId = best.itemId,
                            ebayTitle = best.title,
                            ebayPrice = best.price,
                            ebayShipping = best.shipping,
                            ebayCurrency = best.currency,
                            ebayImageUrl = best.imageUrl,
                            ebayItemUrl = best.itemUrl,
                            ebaySeller = best.seller,
                            ebayCondition = best.condition,
                            ebayCandidateCount = candidates.size,
                            matchMethod = method,
                            confidence = confidence,
                            matchReason = reason,
                            status = status
                        )
                    )
                    if (inserted > 0) {
                        if (status == "AUTO") autoCount++ else reviewCount++
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    errors++
                    consecutiveErrors++
                    log("ERROR", "Fehler bei Produkt", "${product.title.take(60)}: ${e.message}", relatedId = product.productId, isError = true)
                    if (consecutiveErrors >= 5) throw IllegalStateException("Scan abgebrochen nach 5 Fehlern in Folge. Letzter: ${e.message}")
                }
            }
        }

        settingDao.insertOrUpdate((settingDao.getSettingsOnce() ?: s).copy(lastScanRun = System.currentTimeMillis()))

        val message = "$checked geprüft · $autoCount sicher · $reviewCount zum Prüfen · $updated aktualisiert · $discarded verworfen" +
            if (errors > 0) " · $errors Fehler" else ""
        log("MATCH", "Scan beendet", message, isError = false)
        ScanResult(checked, autoCount, reviewCount, discarded, updated, errors, message)
    }

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
        lines += if (geminiService.resolveApiKey(s.geminiApiKey) != null) {
            "Gemini: Schlüssel vorhanden (Modell ${s.geminiModel}) – wird beim Scan geprüft."
        } else {
            "Gemini: kein Schlüssel – Zuordnung nur per Titelvergleich, alles landet in „Prüfen“."
        }

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
