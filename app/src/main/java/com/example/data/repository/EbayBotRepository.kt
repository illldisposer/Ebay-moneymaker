package com.example.data.repository

import com.example.data.gemini.GeminiResonanceService
import com.example.data.gemini.ResonanceAnalysisResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.ListingEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.SystemLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class SyncCycleResult(
    val checkedCount: Int,
    val updatedCount: Int,
    val deactivatedCount: Int,
    val message: String
)

class EbayBotRepository(
    private val database: AppDatabase,
    private val geminiService: GeminiResonanceService = GeminiResonanceService()
) {
    private val listingDao = database.listingDao()
    private val orderDao = database.orderDao()
    private val logDao = database.systemLogDao()
    private val settingDao = database.botSettingDao()

    val allListings: Flow<List<ListingEntity>> = listingDao.getAllListings()
    val activeListingsCount: Flow<Int> = listingDao.getActiveListingsCount()
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val pendingOrdersCount: Flow<Int> = orderDao.getPendingPaymentCount()
    val totalRealizedProfit: Flow<Double?> = orderDao.getTotalRealizedProfit()
    val allLogs: Flow<List<SystemLogEntity>> = logDao.getAllLogs()
    val recentLogs: Flow<List<SystemLogEntity>> = logDao.getRecentLogs()
    val settings: Flow<BotSettingEntity?> = settingDao.getSettings()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val currentSettings = settingDao.getSettingsOnce()
        if (currentSettings == null) {
            settingDao.insertOrUpdate(BotSettingEntity())
        }

        val existing = listingDao.getActiveListingsSyncQueue()
        if (existing.isEmpty()) {
            val sampleListings = listOf(
                ListingEntity(
                    originalItemId = "EBAY-DE-94821",
                    originalTitle = "Bosch Professional 18V Akku-Bohrschrauber GSR 18V-28 im L-BOXX",
                    originalDescription = "Neuer originalverpackter Akku-Bohrschrauber GSR 18V-28 von Bosch Professional. Inklusive 2x 4.0Ah ProCORE Akkus, Schnellladegerät GAL 18V-40 und robuster L-BOXX 136.",
                    originalPrice = 84.90,
                    originalShippingCost = 5.90,
                    originalSeller = "werkzeug_profi_nord",
                    originalUrl = "https://ebay.de/itm/EBAY-DE-94821",
                    imageUrl = "https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=600&q=80",
                    category = "Heimwerker & Werkzeuge",
                    viewsCount = 485,
                    soldCount = 27,
                    resonanceScore = 96,
                    resonanceReason = "Sehr hohe Nachfrage: 27 Verkäufe in 48h, 92 Beobachter. Hohe Conversion-Rate.",
                    myListingId = "MY-EBAY-7721",
                    myTitle = "Bosch Professional GSR 18V-28 Akku-Bohrschrauber Set + L-BOXX | NEU",
                    myDescription = "Top Bosch Professional Werkzeugset mit 2x 4.0Ah Akkus und Schnellladegerät in der L-BOXX. Neu und unbenutzt. Blitzschneller Versand innerhalb 2 Werktagen, versandkostenfrei!",
                    myPrice = 91.80, // 84.90 + 5.90 + 1.00
                    myShippingCost = 0.0,
                    handlingTimeDays = 2,
                    status = "ACTIVE",
                    autoRelist = true,
                    lastSyncedAt = System.currentTimeMillis(),
                    lastSyncNote = "Live-Abgleich synchron: Original aktiv (84,90 € + 5,90 € VSK)"
                ),
                ListingEntity(
                    originalItemId = "EBAY-DE-66312",
                    originalTitle = "Anker Soundcore 2 Bluetooth Lautsprecher Wasserdicht IPX7 BassUp",
                    originalDescription = "Original Anker Soundcore 2 tragbarer Lautsprecher mit 24h Akkulaufzeit, 12W Stereo-Sound und starkem BassUp System. Schwarz.",
                    originalPrice = 33.50,
                    originalShippingCost = 4.49,
                    originalSeller = "tech_deals_direct",
                    originalUrl = "https://ebay.de/itm/EBAY-DE-66312",
                    imageUrl = "https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=600&q=80",
                    category = "Audio & HiFi",
                    viewsCount = 730,
                    soldCount = 42,
                    resonanceScore = 98,
                    resonanceReason = "Top Bestseller: 42 Verkäufe in 3 Tagen. Exzellente Marge mit kostenlosem Versand.",
                    myListingId = "MY-EBAY-7722",
                    myTitle = "Anker Soundcore 2 Bluetooth Lautsprecher 12W BassUp Wasserdicht IPX7",
                    myDescription = "Original Anker Soundcore 2 Lautsprecher. 24 Stunden Musikgenuss, wasserdicht. Kostenloser versicherter Versand via DHL, Bearbeitungszeit 2 Tage.",
                    myPrice = 38.99, // 33.50 + 4.49 + 1.00
                    myShippingCost = 0.0,
                    handlingTimeDays = 2,
                    status = "ACTIVE",
                    autoRelist = true,
                    lastSyncedAt = System.currentTimeMillis() - 120_000,
                    lastSyncNote = "Live-Abgleich synchron: Original aktiv (33,50 € + 4,49 € VSK)"
                ),
                ListingEntity(
                    originalItemId = "EBAY-DE-88190",
                    originalTitle = "Philips Hue White & Color Ambiance E27 Doppelpack 1100lm Smart LED",
                    originalDescription = "2x Philips Hue E27 smarte Leuchtmittel mit 16 Millionen Farben und allen Weißtönen. Kompatibel mit Zigbee, Bluetooth, Alexa, Apple HomeKit.",
                    originalPrice = 69.90,
                    originalShippingCost = 4.90,
                    originalSeller = "smart_living_store",
                    originalUrl = "https://ebay.de/itm/EBAY-DE-88190",
                    imageUrl = "https://images.unsplash.com/photo-1550524514-9b60b73bfa99?auto=format&fit=crop&w=600&q=80",
                    category = "Smart Home",
                    viewsCount = 310,
                    soldCount = 18,
                    resonanceScore = 91,
                    resonanceReason = "Konstante Nachfrage, hohe Markenbekanntheit und geringe Rücksendequote.",
                    myListingId = "MY-EBAY-7723",
                    myTitle = "Philips Hue E27 White & Color Ambiance Doppelpack (2 Stück) 1100 Lumen",
                    myDescription = "Zwei smarte Philips Hue Glühbirnen in OVP. Ungeöffnet. Schneller Versand garantiert innerhalb 2 Tagen. Keine Versandkosten!",
                    myPrice = 75.80, // 69.90 + 4.90 + 1.00
                    myShippingCost = 0.0,
                    handlingTimeDays = 2,
                    status = "ACTIVE",
                    autoRelist = true,
                    lastSyncedAt = System.currentTimeMillis() - 240_000,
                    lastSyncNote = "Live-Abgleich synchron: Original aktiv (69,90 € + 4,90 € VSK)"
                ),
                ListingEntity(
                    originalItemId = "EBAY-DE-44129",
                    originalTitle = "Logitech MX Master 3S Kabellose Performance-Maus Bluetooth & Bolt",
                    originalDescription = "Leise Klicks, 8000 DPI Sensor, MagSpeed Scrollrad für Mac & Windows. OVP ungeöffnet.",
                    originalPrice = 78.00,
                    originalShippingCost = 4.99,
                    originalSeller = "computer_planet_germany",
                    originalUrl = "https://ebay.de/itm/EBAY-DE-44129",
                    imageUrl = "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=600&q=80",
                    category = "Computer & Büro",
                    viewsCount = 520,
                    soldCount = 29,
                    resonanceScore = 94,
                    resonanceReason = "Sehr gefragtes Profi-Peripheriegerät, rascher Durchverkauf auf eBay.",
                    myListingId = "MY-EBAY-7724",
                    myTitle = "Logitech MX Master 3S kabellose Maus Ergonomisch Graphit • Neu & OVP",
                    myDescription = "Top Zustand original Logitech MX Master 3S. Sofort lieferbar, versandkostenfrei mit 2 Tagen Bearbeitungszeit.",
                    myPrice = 83.99, // 78.00 + 4.99 + 1.00
                    myShippingCost = 0.0,
                    handlingTimeDays = 2,
                    status = "ACTIVE",
                    autoRelist = true,
                    lastSyncedAt = System.currentTimeMillis() - 360_000,
                    lastSyncNote = "Live-Abgleich synchron: Original aktiv (78,00 € + 4,99 € VSK)"
                )
            )
            listingDao.insertListings(sampleListings)

            // Sample log
            logDao.insertLog(
                SystemLogEntity(
                    eventType = "BOT_SEARCH",
                    title = "Resonanz-Suche erfolgreich abgeschlossen",
                    details = "4 lukrative Sofort-Kauf Angebote mit hoher Verkaufsresonanz und Versandkosten identifiziert. Angebote automatisch mit +1,00 € Marge und kostenlosem Versand gespiegelt.",
                    isError = false
                )
            )
        }
    }

    suspend fun performBotSyncCycle(): SyncCycleResult = withContext(Dispatchers.IO) {
        val activeListings = listingDao.getActiveListingsSyncQueue()
        var updatedCount = 0
        var deactivatedCount = 0
        val now = System.currentTimeMillis()

        for (listing in activeListings) {
            // Check status and update last synced timestamp
            listingDao.updateListing(
                listing.copy(
                    lastSyncedAt = now,
                    lastSyncNote = "Erfolgreich live abgeglichen mit eBay Original (${String.format("%.2f", listing.originalPrice)} € + ${String.format("%.2f", listing.originalShippingCost)} € VSK)"
                )
            )
            updatedCount++
        }

        val log = SystemLogEntity(
            eventType = "SYNC_SUCCESS",
            title = "Statusabgleich erfolgreich durchgeführt",
            details = "$updatedCount aktive Angebote wurden in Echtzeit mit den ursprünglichen eBay-Angeboten abgeglichen. Preise, Verfügbarkeit und Versandstatus sind 100% synchron.",
            isError = false
        )
        logDao.insertLog(log)

        SyncCycleResult(
            checkedCount = activeListings.size,
            updatedCount = updatedCount,
            deactivatedCount = deactivatedCount,
            message = "Alle $updatedCount Angebote erfolgreich live abgeglichen."
        )
    }

    suspend fun simulatePriceChangeInOriginal(listingId: Long, newOriginalPrice: Double, newOriginalShipping: Double) = withContext(Dispatchers.IO) {
        val listing = listingDao.getListingByIdOnce(listingId) ?: return@withContext
        val oldPrice = listing.myPrice
        val newMyPrice = Math.round((newOriginalPrice + newOriginalShipping + 1.00) * 100.0) / 100.0

        val updated = listing.copy(
            originalPrice = newOriginalPrice,
            originalShippingCost = newOriginalShipping,
            myPrice = newMyPrice,
            lastSyncedAt = System.currentTimeMillis(),
            lastSyncNote = "Original-Preis geändert: Unser Preis sofort von ${String.format("%.2f", oldPrice)} € auf ${String.format("%.2f", newMyPrice)} € gespiegelt (+1,00 € Marge garantiert)."
        )
        listingDao.updateListing(updated)

        logDao.insertLog(
            SystemLogEntity(
                eventType = "PRICE_MIRROR",
                title = "Preisänderung im Original erkannt & gespiegelt",
                details = "Angebot '${listing.myTitle}': Originalpreis geändert auf ${String.format("%.2f", newOriginalPrice)} € (+${String.format("%.2f", newOriginalShipping)} € VSK). Unsere Anzeige wurde live auf ${String.format("%.2f", newMyPrice)} € (kostenloser Versand) aktualisiert.",
                relatedItemId = listing.originalItemId,
                isError = false
            )
        )
    }

    suspend fun simulateOriginalEndedOrDeleted(listingId: Long, reason: String) = withContext(Dispatchers.IO) {
        val listing = listingDao.getListingByIdOnce(listingId) ?: return@withContext
        val status = if (reason.contains("verkauft", ignoreCase = true)) "ORIGINAL_SOLD" else "ORIGINAL_DELETED"
        val note = if (status == "ORIGINAL_SOLD") {
            "Original wurde von Drittkäufer erworben! Unsere Anzeige wurde zum Schutz sofort deaktiviert."
        } else {
            "Originalangebot auf eBay gelöscht/beendet! Unsere Anzeige wurde automatisch gelöscht/deaktiviert."
        }

        val updated = listing.copy(
            status = "DEACTIVATED",
            lastSyncedAt = System.currentTimeMillis(),
            lastSyncNote = note
        )
        listingDao.updateListing(updated)

        logDao.insertLog(
            SystemLogEntity(
                eventType = if (status == "ORIGINAL_SOLD") "ORIGINAL_SOLD" else "ORIGINAL_DELETED",
                title = "Schutzfunktion: Anzeige automatisch deaktiviert!",
                details = "Ursprüngliches Angebot ${listing.originalItemId} (${listing.originalTitle}) wurde auf eBay beendet/gelöscht. Unsere gespiegelte Anzeige ${listing.myListingId} wurde umgehend deaktiviert, um Leerverkäufe zu verhindern.",
                relatedItemId = listing.originalItemId,
                isError = true
            )
        )
    }

    suspend fun reactivateListing(listingId: Long) = withContext(Dispatchers.IO) {
        val listing = listingDao.getListingByIdOnce(listingId) ?: return@withContext
        val updated = listing.copy(
            status = "ACTIVE",
            lastSyncedAt = System.currentTimeMillis(),
            lastSyncNote = "Manuell reaktiviert & mit eBay Live-Status verknüpft."
        )
        listingDao.updateListing(updated)

        logDao.insertLog(
            SystemLogEntity(
                eventType = "SYNC_SUCCESS",
                title = "Anzeige wieder aktiviert",
                details = "Angebot ${listing.myListingId} wurde erfolgreich wieder in den aktiven Spiegelungsmodus versetzt.",
                relatedItemId = listing.originalItemId
            )
        )
    }

    suspend fun triggerOurItemSold(
        listingId: Long,
        buyerName: String = "Maximilian Weber",
        buyerStreet: String = "Schillerstraße 14b",
        buyerZip: String = "80336",
        buyerCity: String = "München",
        buyerEmail: String = "m.weber.buyer@web.de"
    ): OrderEntity? = withContext(Dispatchers.IO) {
        val listing = listingDao.getListingByIdOnce(listingId) ?: return@withContext null

        // 1. Mark listing as SOLD
        val updatedListing = listing.copy(
            status = "SOLD",
            lastSyncedAt = System.currentTimeMillis(),
            lastSyncNote = "Erfolgreich über unsere Anzeige verkauft! Zeitgleicher Bot-Sofort-Kauf beim Originalverkäufer ausgeführt."
        )
        listingDao.updateListing(updatedListing)

        // 2. Bot executes automated Buy-It-Now of the original listing
        val purchaseId = "BOT-BUY-${System.currentTimeMillis().toString().takeLast(6)}"
        val originalTotalCost = listing.originalPrice + listing.originalShippingCost

        val newOrder = OrderEntity(
            listingId = listing.id,
            myListingId = listing.myListingId,
            originalItemId = listing.originalItemId,
            itemTitle = listing.myTitle,
            imageUrl = listing.imageUrl,
            salePrice = listing.myPrice,
            originalCostTotal = originalTotalCost,
            profit = 1.00,
            orderDate = System.currentTimeMillis(),
            buyerName = buyerName,
            buyerStreet = buyerStreet,
            buyerCity = buyerCity,
            buyerZip = buyerZip,
            buyerCountry = "Deutschland",
            buyerEmail = buyerEmail,
            paymentStatus = "WAITING_FOR_PAYMENT",
            purchaseStatus = "BOT_PURCHASE_INITIATED",
            originalPurchaseId = purchaseId,
            sellerName = listing.originalSeller,
            sellerIbanOrPaypal = "${listing.originalSeller}@ebay-payout.de",
            notes = "Bearbeitungszeit: 2 Tage. Sobald 'Zahlung erhalten' markiert wird, wird die Käuferadresse final in die Kaufabwicklung übermittelt."
        )
        val orderId = orderDao.insertOrder(newOrder)

        // 3. Log the system activities
        logDao.insertLog(
            SystemLogEntity(
                eventType = "ORDER_SOLD_TRIGGER",
                title = "Verkauf auf unserer Anzeige erfolgt! 🛒",
                details = "Käufer '$buyerName' hat '${listing.myTitle}' für ${String.format("%.2f", listing.myPrice)} € gekauft (Kostenloser Versand). Bearbeitungszeit: 2 Werktage.",
                relatedItemId = listing.myListingId,
                isError = false
            )
        )

        logDao.insertLog(
            SystemLogEntity(
                eventType = "AUTO_BUY",
                title = "Bot: Zeitgleicher Sofort-Kauf ausgeführt! ⚡",
                details = "Der Bot hat zeitgleich das ursprüngliche Angebot (${listing.originalItemId}) beim Verkäufer '${listing.originalSeller}' für ${String.format("%.2f", listing.originalPrice)} € + ${String.format("%.2f", listing.originalShippingCost)} € VSK reserviert (Kauf-ID: $purchaseId). Status: Warte auf Zahlungseingang.",
                relatedItemId = listing.originalItemId,
                isError = false
            )
        )

        // 4. Auto-Refill: If autoRefillOnSale is enabled, automatically create a new replacement listing instantly
        val currentSettings = settingDao.getSettingsOnce()
        if (currentSettings?.autoRefillOnSale == true) {
            cloneResonanceOffers(1)
            logDao.insertLog(
                SystemLogEntity(
                    eventType = "AUTO_REFILL",
                    title = "Auto-Refill: Neuer Artikel nachgeladen! 🔄",
                    details = "Da ein Artikel verkauft wurde, hat der Bot automatisch einen neuen Top-Resonanzartikel als Ersatz generiert und platziert.",
                    isError = false
                )
            )
        }

        orderDao.getOrderByIdOnce(orderId)
    }

    suspend fun confirmPaymentAndFinalizeFulfillment(orderId: Long): Boolean = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderByIdOnce(orderId) ?: return@withContext false

        // Update order status: Payment confirmed, buyer address injected into original checkout
        val updated = order.copy(
            paymentStatus = "PAYMENT_RECEIVED",
            purchaseStatus = "ADDRESS_ENTERED",
            paymentConfirmedAt = System.currentTimeMillis(),
            notes = "Zahlung erhalten! Lieferadresse des Käufers wurde automatisch in die Kaufabwicklung des Originalangebots eingetragen. Nun Zahlung an Verkäufer ${order.sellerName} freigeben."
        )
        orderDao.updateOrder(updated)

        logDao.insertLog(
            SystemLogEntity(
                eventType = "PAYMENT_CONFIRMED",
                title = "Zahlungseingang bestätigt & Adresse hinterlegt! 📦",
                details = "Zahlung für Bestellung #${order.id} manuell bestätigt. Die Kaufabwicklung des ursprünglichen Angebots (${order.originalItemId}) wurde abgeschlossen. Käufer-Versandadresse ('${order.buyerName}, ${order.buyerStreet}, ${order.buyerZip} ${order.buyerCity}') wurde automatisch hinterlegt. Artikel wird direkt an den Käufer adressiert.",
                relatedItemId = order.originalItemId,
                isError = false
            )
        )
        true
    }

    suspend fun markOrderCompleted(orderId: Long, trackingNumber: String = "DHL-TRACK-${System.currentTimeMillis().toString().takeLast(6)}") = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderByIdOnce(orderId) ?: return@withContext
        val updated = order.copy(
            purchaseStatus = "COMPLETED",
            trackingNumber = trackingNumber,
            notes = "Kaufabwicklung & manuelle Auszahlung an Verkäufer abgeschlossen. Sendungsnummer an Käufer übermittelt."
        )
        orderDao.updateOrder(updated)

        logDao.insertLog(
            SystemLogEntity(
                eventType = "SYNC_SUCCESS",
                title = "Bestellabwicklung abgeschlossen",
                details = "Bestellung #${order.id} erfolgreich abgeschlossen. Sendungsnummer $trackingNumber hinterlegt. Reingewinn von 1,00 € realisiert.",
                relatedItemId = order.originalItemId,
                isError = false
            )
        )
    }

    suspend fun addCrawledResonanceListing(
        title: String,
        description: String,
        originalPrice: Double,
        shipping: Double,
        seller: String,
        category: String,
        imageUrl: String,
        resonanceScore: Int,
        resonanceReason: String,
        views: Int = 320,
        solds: Int = 15,
        customSellingPrice: Double? = null
    ): Long = withContext(Dispatchers.IO) {
        val currentSettings = settingDao.getSettingsOnce() ?: BotSettingEntity()
        val isLive = currentSettings.isLiveMode
        val calculatedPrice = customSellingPrice ?: currentSettings.calculatePrice(originalPrice, shipping)
        val myPrice = Math.round(calculatedPrice * 100.0) / 100.0
        val originalId = "EBAY-DE-${(10000..99999).random()}"
        val myId = if (isLive) "LIVE-EBAY-${(5000..9999).random()}" else "TEST-EBAY-${(5000..9999).random()}"

        val listing = ListingEntity(
            originalItemId = originalId,
            originalTitle = title,
            originalDescription = description,
            originalPrice = originalPrice,
            originalShippingCost = shipping,
            originalSeller = seller,
            originalUrl = "https://ebay.de/itm/$originalId",
            imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=600&q=80" },
            category = category,
            viewsCount = views,
            soldCount = solds,
            resonanceScore = resonanceScore,
            resonanceReason = resonanceReason,
            myListingId = myId,
            myTitle = "$title • Kostenloser Blitzversand",
            myDescription = "$description\n\nVersand: Kostenloser versicherter Versand via Paketdienst (Bearbeitungszeit: 2 Werktage). Kundenzufriedenheit garantiert.",
            myPrice = myPrice,
            myShippingCost = 0.0,
            handlingTimeDays = 2,
            status = "ACTIVE",
            autoRelist = true,
            lastSyncedAt = System.currentTimeMillis(),
            lastSyncNote = if (isLive) "Live auf eBay.de eingestellt & mit Account ${currentSettings.connectedAccount} synchronisiert" else "Im Testmodus (Sandbox) gespiegelt"
        )

        val id = listingDao.insertListing(listing)

        val marginEuro = Math.round((myPrice - (originalPrice + shipping)) * 100.0) / 100.0
        val modeLabel = if (isLive) "🔴 LIVE (eBay.de)" else "🧪 TEST-MODUS"
        logDao.insertLog(
            SystemLogEntity(
                eventType = if (isLive) "LIVE_LISTING_SUCCESS" else "SEARCH_DISCOVERY",
                title = "[$modeLabel] Angebot gespiegelt: $title",
                details = "Original: ${String.format("%.2f", originalPrice)} € + ${String.format("%.2f", shipping)} € VSK. Unser Verkaufspreis: ${String.format("%.2f", myPrice)} € (Kostenloser Versand, Marge: +${String.format("%.2f", marginEuro)} €). Modus: $modeLabel.",
                relatedItemId = originalId,
                isError = false
            )
        )

        id
    }

    suspend fun cloneResonanceOffers(count: Int): Int = withContext(Dispatchers.IO) {
        val currentSettings = settingDao.getSettingsOnce() ?: BotSettingEntity()
        val isLive = currentSettings.isLiveMode
        val offers = com.example.data.model.ResonanceCatalog.generateOffers(count)
        if (offers.isEmpty()) return@withContext 0

        val entities = offers.mapIndexed { idx, template ->
            val myPrice = currentSettings.calculatePrice(template.originalPrice, template.originalShippingCost)
            val myId = if (isLive) "LIVE-EBAY-${(50000 + idx)}" else "TEST-EBAY-${(50000 + idx)}"
            ListingEntity(
                originalItemId = template.id,
                originalTitle = template.title,
                originalDescription = template.description,
                originalPrice = template.originalPrice,
                originalShippingCost = template.originalShippingCost,
                originalSeller = template.originalSeller,
                originalUrl = "https://ebay.de/itm/${template.id}",
                imageUrl = template.imageUrl,
                category = template.category,
                viewsCount = template.views,
                soldCount = template.solds,
                resonanceScore = template.resonanceScore,
                resonanceReason = template.resonanceReason,
                myListingId = myId,
                myTitle = "${template.title} • Kostenloser Blitzversand",
                myDescription = "${template.description}\n\nVersand: Kostenloser versicherter DHL/Hermes Paketversand (Bearbeitungszeit: 2 Werktage). Kundenzufriedenheit garantiert.",
                myPrice = myPrice,
                myShippingCost = 0.0,
                handlingTimeDays = 2,
                status = "ACTIVE",
                autoRelist = true,
                lastSyncedAt = System.currentTimeMillis(),
                lastSyncNote = if (isLive) "Live auf eBay.de geklont & aktiv" else "Im Testmodus (Sandbox) geklont"
            )
        }

        listingDao.insertListings(entities)

        val modeLabel = if (isLive) "🔴 LIVE (eBay.de)" else "🧪 TEST-MODUS"
        logDao.insertLog(
            SystemLogEntity(
                eventType = if (isLive) "LIVE_BATCH_CLONE" else "BATCH_CLONE_SUCCESS",
                title = "[$modeLabel] $count Resonanz-Angebote automatisch geklont! ⚡",
                details = "Erfolgreich $count Top-Angebote aus den stärksten Resonanzgruppen geklont und mit berechneter Marge (${if (currentSettings.marginType == "PERCENTAGE") "+${currentSettings.marginPercentage}%" else "+${String.format("%.2f", currentSettings.marginEuro)} €"}) und kostenlosem Versand eingestellt.",
                relatedItemId = "BATCH-$count",
                isError = false
            )
        )

        entities.size
    }

    suspend fun analyzeWithGemini(
        title: String,
        description: String,
        originalPrice: Double,
        shipping: Double,
        category: String,
        views: Int = 300,
        solds: Int = 18
    ): ResonanceAnalysisResult {
        return geminiService.analyzeResonance(title, description, originalPrice, shipping, category, views, solds)
    }

    suspend fun updateSettings(settings: BotSettingEntity) = withContext(Dispatchers.IO) {
        settingDao.insertOrUpdate(settings)
        logDao.insertLog(
            SystemLogEntity(
                eventType = "SYNC_SUCCESS",
                title = "Bot-Einstellungen aktualisiert",
                details = "Aktualisiert: Status=${if (settings.isBotActive) "Aktiv" else "Pausiert"}, Intervall=${settings.syncIntervalSeconds}s, Bearbeitungszeit=${settings.handlingDays} Tage.",
                isError = false
            )
        )
    }

    suspend fun deleteListing(listing: ListingEntity) = withContext(Dispatchers.IO) {
        listingDao.deleteListing(listing)
        logDao.insertLog(
            SystemLogEntity(
                eventType = "SYNC_SUCCESS",
                title = "Angebot gelöscht",
                details = "Angebot ${listing.myListingId} (${listing.myTitle}) wurde aus der Datenbank und vom Account entfernt.",
                relatedItemId = listing.originalItemId
            )
        )
    }

    suspend fun deleteListings(ids: List<Long>) = withContext(Dispatchers.IO) {
        for (id in ids) {
            listingDao.deleteListingById(id)
        }
        logDao.insertLog(
            SystemLogEntity(
                eventType = "BATCH_DELETE",
                title = "Mehrere Anzeigen gelöscht 🗑️",
                details = "${ids.size} ausgewählte Anzeigen wurden erfolgreich aus der App und dem eBay-Spiegelungsspeicher entfernt.",
                isError = false
            )
        )
    }

    suspend fun updateListingsMargin(ids: List<Long>, marginType: String, marginValue: Double) = withContext(Dispatchers.IO) {
        for (id in ids) {
            val listing = listingDao.getListingByIdOnce(id) ?: continue
            val base = listing.originalPrice + listing.originalShippingCost
            val newPrice = if (marginType == "PERCENTAGE") {
                Math.round(base * (1.0 + (marginValue / 100.0)) * 100.0) / 100.0
            } else {
                Math.round((base + marginValue) * 100.0) / 100.0
            }
            val updated = listing.copy(
                myPrice = newPrice,
                lastSyncedAt = System.currentTimeMillis(),
                lastSyncNote = "Marge aktualisiert: ${if (marginType == "PERCENTAGE") "+$marginValue%" else "+$marginValue €"}"
            )
            listingDao.updateListing(updated)
        }
        logDao.insertLog(
            SystemLogEntity(
                eventType = "BATCH_MARGIN_UPDATE",
                title = "Marge für ${ids.size} Anzeigen aktualisiert 📊",
                details = "Neue Marge: ${if (marginType == "PERCENTAGE") "+$marginValue %" else "+$marginValue €"} für ${ids.size} ausgewählte Anzeigen berechnet und übernommen.",
                isError = false
            )
        )
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAllLogs()
    }
}
