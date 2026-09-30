package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.ResonanceAnalysisResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.ListingEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.SystemLogEntity
import com.example.data.repository.EbayBotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BotUiState(
    val listings: List<ListingEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val logs: List<SystemLogEntity> = emptyList(),
    val settings: BotSettingEntity = BotSettingEntity(),
    val activeCount: Int = 0,
    val pendingOrderCount: Int = 0,
    val totalProfit: Double = 0.0,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val listingFilter: String = "ALL", // "ALL", "ACTIVE", "DEACTIVATED", "SOLD"
    val logFilter: String = "ALL",
    val notificationBanner: String? = null,
    val isErrorNotification: Boolean = false,
    val isAiAnalyzing: Boolean = false,
    val aiAnalysisResult: ResonanceAnalysisResult? = null
)

class EbayBotViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = EbayBotRepository(database)

    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    private val _listingFilter = MutableStateFlow("ALL")
    private val _logFilter = MutableStateFlow("ALL")
    private val _notificationBanner = MutableStateFlow<String?>(null)
    private val _isErrorNotification = MutableStateFlow(false)
    private val _isAiAnalyzing = MutableStateFlow(false)
    private val _aiAnalysisResult = MutableStateFlow<ResonanceAnalysisResult?>(null)

    private var autoSyncJob: Job? = null

    val uiState: StateFlow<BotUiState> = combine(
        repository.allListings,
        repository.allOrders,
        repository.allLogs,
        repository.settings,
        repository.activeListingsCount,
        repository.pendingOrdersCount,
        repository.totalRealizedProfit,
        _isSyncing,
        _lastSyncTime,
        _listingFilter,
        _logFilter,
        _notificationBanner,
        _isErrorNotification,
        _isAiAnalyzing,
        _aiAnalysisResult
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val listings = (args[0] as? List<ListingEntity>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val orders = (args[1] as? List<OrderEntity>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val logs = (args[2] as? List<SystemLogEntity>) ?: emptyList()
        val settings = (args[3] as? BotSettingEntity) ?: BotSettingEntity()
        val activeCount = (args[4] as? Int) ?: 0
        val pendingCount = (args[5] as? Int) ?: 0
        val totalProfit = (args[6] as? Double) ?: 0.0
        val isSyncing = (args[7] as? Boolean) ?: false
        val lastSync = (args[8] as? Long) ?: System.currentTimeMillis()
        val lFilter = (args[9] as? String) ?: "ALL"
        val logF = (args[10] as? String) ?: "ALL"
        val notif = args[11] as? String
        val isErr = (args[12] as? Boolean) ?: false
        val isAi = (args[13] as? Boolean) ?: false
        val aiRes = args[14] as? ResonanceAnalysisResult

        val filteredListings = when (lFilter) {
            "ACTIVE" -> listings.filter { it.status == "ACTIVE" }
            "DEACTIVATED" -> listings.filter { it.status == "DEACTIVATED" || it.status == "ORIGINAL_DELETED" || it.status == "ORIGINAL_SOLD" }
            "SOLD" -> listings.filter { it.status == "SOLD" }
            else -> listings
        }

        val filteredLogs = when (logF) {
            "SYNC" -> logs.filter { it.eventType == "SYNC_SUCCESS" }
            "PRICE" -> logs.filter { it.eventType == "PRICE_MIRROR" }
            "AUTO_BUY" -> logs.filter { it.eventType == "AUTO_BUY" }
            "PAYMENT" -> logs.filter { it.eventType == "PAYMENT_CONFIRMED" }
            "DELETED" -> logs.filter { it.eventType == "ORIGINAL_DELETED" || it.eventType == "ORIGINAL_SOLD" }
            "ERROR" -> logs.filter { it.isError }
            else -> logs
        }

        BotUiState(
            listings = filteredListings,
            orders = orders,
            logs = filteredLogs,
            settings = settings,
            activeCount = activeCount,
            pendingOrderCount = pendingCount,
            totalProfit = totalProfit,
            isSyncing = isSyncing,
            lastSyncTime = lastSync,
            listingFilter = lFilter,
            logFilter = logF,
            notificationBanner = notif,
            isErrorNotification = isErr,
            isAiAnalyzing = isAi,
            aiAnalysisResult = aiRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BotUiState()
    )

    init {
        startAutoSyncWorker()
    }

    private fun startAutoSyncWorker() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                val currentSettings = uiState.value.settings
                val intervalSeconds = if (currentSettings.syncIntervalSeconds in 5..3600) currentSettings.syncIntervalSeconds else 30
                delay(intervalSeconds * 1000L)

                if (currentSettings.isBotActive) {
                    performLiveSyncCycle(isSilent = true)
                }
            }
        }
    }

    fun performLiveSyncCycle(isSilent: Boolean = false) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = repository.performBotSyncCycle()
                _lastSyncTime.value = System.currentTimeMillis()
                if (!isSilent || uiState.value.settings.notificationsEnabled) {
                    showNotification("✓ Live-Sync: ${result.message}", isError = false)
                }
            } catch (e: Exception) {
                showNotification("Fehler beim Statusabgleich: ${e.message}", isError = true)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setListingFilter(filter: String) {
        _listingFilter.value = filter
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun simulatePriceChange(listingId: Long, newOriginalPrice: Double, newOriginalShipping: Double) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.simulatePriceChangeInOriginal(listingId, newOriginalPrice, newOriginalShipping)
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false
            showNotification(
                "⚡ Preisspiegelung aktiv: Originalpreis geändert auf ${String.format("%.2f", newOriginalPrice)} € (+${String.format("%.2f", newOriginalShipping)} € VSK). Unser Angebot sofort angepasst!",
                isError = false
            )
        }
    }

    fun simulateOriginalDeletion(listingId: Long, reason: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.simulateOriginalEndedOrDeleted(listingId, reason)
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false
            showNotification(
                "⚠️ Schutzfunktion ausgelöst: Original wurde $reason! Unsere Anzeige wurde zum Schutz vor Leerverkäufen sofort deaktiviert.",
                isError = true
            )
        }
    }

    fun reactivateListing(listingId: Long) {
        viewModelScope.launch {
            repository.reactivateListing(listingId)
            showNotification("Anzeige wieder aktiviert und mit Live-Sync verknüpft.", isError = false)
        }
    }

    fun simulateOurSale(listingId: Long) {
        viewModelScope.launch {
            _isSyncing.value = true
            val order = repository.triggerOurItemSold(listingId)
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false

            if (order != null) {
                showNotification(
                    "🎉 VERKAUF! Bot hat zeitgleich Sofort-Kauf für Original-Angebot ausgelöst. Bearbeitungszeit: 2 Tage. Warte auf Bestätigung des Zahlungseingangs.",
                    isError = false
                )
            }
        }
    }

    fun confirmPaymentAndFinalize(orderId: Long) {
        viewModelScope.launch {
            val success = repository.confirmPaymentAndFinalizeFulfillment(orderId)
            if (success) {
                showNotification(
                    "📦 Zahlung erhalten! Kaufabwicklung des Originalangebots abgeschlossen. Käufer-Lieferadresse erfolgreich hinterlegt. Nun manuelle Zahlung an Verkäufer veranlassen.",
                    isError = false
                )
            }
        }
    }

    fun markOrderCompleted(orderId: Long, trackingNumber: String) {
        viewModelScope.launch {
            repository.markOrderCompleted(orderId, trackingNumber)
            showNotification("✓ Bestellung abgeschlossen & Sendungsnummer übermittelt!", isError = false)
        }
    }

    fun crawlAndAddListing(
        title: String,
        description: String,
        originalPrice: Double,
        shipping: Double,
        seller: String,
        category: String,
        imageUrl: String,
        resonanceScore: Int,
        resonanceReason: String,
        customSellingPrice: Double? = null
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.addCrawledResonanceListing(
                title = title,
                description = description,
                originalPrice = originalPrice,
                shipping = shipping,
                seller = seller,
                category = category,
                imageUrl = imageUrl,
                resonanceScore = resonanceScore,
                resonanceReason = resonanceReason,
                customSellingPrice = customSellingPrice
            )
            _isSyncing.value = false
            val currentSettings = uiState.value.settings
            val modeName = if (currentSettings.isLiveMode) "🔴 Live auf eBay.de" else "🧪 Im Testmodus"
            showNotification(
                "✨ Angebot $modeName gespiegelt (kostenloser Versand, 2 Tage Bearbeitungszeit)!",
                isError = false
            )
        }
    }

    fun toggleLiveMode(isLive: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(isLiveMode = isLive)
            repository.updateSettings(updated)
            if (isLive) {
                showNotification("🔴 Live-Modus aktiviert! Bot interagiert jetzt direkt mit www.ebay.de.", isError = false)
            } else {
                showNotification("🧪 Test-Modus aktiviert. Simulation ohne Risiko & ohne echte eBay-Gebühren.", isError = false)
            }
        }
    }

    fun recordEbayLogin(username: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(
                isEbayLoggedIn = true,
                ebayUsername = username,
                connectedAccount = username
            )
            repository.updateSettings(updated)
            showNotification("✓ Erfolgreich mit originaler eBay.de Seite als '$username' verbunden!", isError = false)

            // Automatically clone top resonance offers if enabled
            if (current.autoCloneOnLogin && current.autoCloneCount > 0) {
                cloneTopResonanceOffers(current.autoCloneCount)
            }
        }
    }

    fun cloneTopResonanceOffers(customCount: Int? = null) {
        viewModelScope.launch {
            val countToClone = customCount ?: uiState.value.settings.autoCloneCount
            if (countToClone <= 0) {
                showNotification("Klonen übersprungen: Anzahl ist auf 0 eingestellt.", isError = false)
                return@launch
            }
            _isSyncing.value = true
            val clonedCount = repository.cloneResonanceOffers(countToClone)
            _isSyncing.value = false
            val modeName = if (uiState.value.settings.isLiveMode) "🔴 Live auf eBay.de" else "🧪 Im Testmodus"
            showNotification(
                "🚀 $clonedCount Top-Resonanzangebote erfolgreich $modeName geklont und eingestellt!",
                isError = false
            )
        }
    }

    fun analyzeWithGemini(
        title: String,
        description: String,
        price: Double,
        shipping: Double,
        category: String
    ) {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            val result = repository.analyzeWithGemini(title, description, price, shipping, category)
            _aiAnalysisResult.value = result
            _isAiAnalyzing.value = false
        }
    }

    fun clearAiResult() {
        _aiAnalysisResult.value = null
    }

    fun updateSettings(newSettings: BotSettingEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            startAutoSyncWorker()
            showNotification("Einstellungen gespeichert. Bot-Sync aktualisiert.", isError = false)
        }
    }

    fun deleteListing(listing: ListingEntity) {
        viewModelScope.launch {
            repository.deleteListing(listing)
            showNotification("Angebot entfernt.", isError = false)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            showNotification("Systemprotokoll geleert.", isError = false)
        }
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notificationBanner.value = message
        _isErrorNotification.value = isError
    }

    fun dismissNotification() {
        _notificationBanner.value = null
    }
}
