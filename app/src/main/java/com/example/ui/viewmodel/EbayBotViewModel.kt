package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.GapEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.SystemLogEntity
import com.example.data.repository.EbayBotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BotUiState(
    val matches: List<MatchEntity> = emptyList(),
    val reviewQueue: List<MatchEntity> = emptyList(),
    val reviewCount: Int = 0,
    val logs: List<SystemLogEntity> = emptyList(),
    val settings: BotSettingEntity = BotSettingEntity(),
    val isScanning: Boolean = false,
    val scanProgress: String? = null,
    val isTestingConnections: Boolean = false,
    val connectionTestResult: String? = null,
    val logFilter: String = "ALL",
    val notificationBanner: String? = null,
    val isErrorNotification: Boolean = false
)

/**
 * Zustand des Tabs "Marktlücken".
 * filter: OPEN, GAP (nur eBay), BOTH (auf beiden), UNCLEAR (unklar), ALL (alle).
 */
data class GapUiState(
    val gaps: List<GapEntity> = emptyList(),
    val filter: String = "OPEN",
    val isSearching: Boolean = false,
    val progress: String? = null,
    val message: String? = null
)

class EbayBotViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = EbayBotRepository(database)

    private val _gapFilter = MutableStateFlow("OPEN")
    private val _gapSearching = MutableStateFlow(false)
    private val _gapProgress = MutableStateFlow<String?>(null)
    private val _gapMessage = MutableStateFlow<String?>(null)
    private var gapJob: Job? = null

    val gapState: StateFlow<GapUiState> = combine(
        repository.gaps,
        _gapFilter,
        _gapSearching,
        _gapProgress,
        _gapMessage
    ) { gaps, filter, searching, progress, message ->
        GapUiState(
            gaps = gaps,
            filter = filter,
            isSearching = searching,
            progress = progress,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GapUiState()
    )

    private val _isScanning = MutableStateFlow(false)
    private val _scanProgress = MutableStateFlow<String?>(null)
    private val _isTesting = MutableStateFlow(false)
    private val _connectionResult = MutableStateFlow<String?>(null)
    private val _logFilter = MutableStateFlow("ALL")
    private val _notificationBanner = MutableStateFlow<String?>(null)
    private val _isErrorNotification = MutableStateFlow(false)

    private var scanJob: Job? = null

    val uiState: StateFlow<BotUiState> = combine(
        repository.scannerMatches,
        repository.reviewQueue,
        repository.reviewCount,
        repository.allLogs,
        repository.settings,
        _isScanning,
        _scanProgress,
        _isTesting,
        _connectionResult,
        _logFilter,
        _notificationBanner,
        _isErrorNotification
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val matches = (args[0] as? List<MatchEntity>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val review = (args[1] as? List<MatchEntity>) ?: emptyList()
        val reviewCount = (args[2] as? Int) ?: 0
        @Suppress("UNCHECKED_CAST")
        val logs = (args[3] as? List<SystemLogEntity>) ?: emptyList()
        val settings = (args[4] as? BotSettingEntity) ?: BotSettingEntity()
        val isScanning = (args[5] as? Boolean) ?: false
        val progress = args[6] as? String
        val isTesting = (args[7] as? Boolean) ?: false
        val connResult = args[8] as? String
        val logF = (args[9] as? String) ?: "ALL"
        val notif = args[10] as? String
        val isErr = (args[11] as? Boolean) ?: false

        val filteredLogs = when (logF) {
            "SCAN" -> logs.filter { it.eventType == "SCAN" || it.eventType == "MATCH" }
            "REVIEW" -> logs.filter { it.eventType == "REVIEW" || it.eventType == "THRESHOLD" }
            "CONNECTION" -> logs.filter { it.eventType == "CONNECTION" }
            "ERROR" -> logs.filter { it.isError }
            else -> logs
        }

        BotUiState(
            matches = matches,
            reviewQueue = review,
            reviewCount = reviewCount,
            logs = filteredLogs,
            settings = settings,
            isScanning = isScanning,
            scanProgress = progress,
            isTestingConnections = isTesting,
            connectionTestResult = connResult,
            logFilter = logF,
            notificationBanner = notif,
            isErrorNotification = isErr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BotUiState()
    )

    fun startScan() {
        if (_isScanning.value) return
        scanJob = viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = "Scan startet …"
            try {
                val result = repository.runScan { progress -> _scanProgress.value = progress }
                showNotification("✓ Scan fertig: ${result.message}", isError = false)
            } catch (e: CancellationException) {
                showNotification("Scan gestoppt.", isError = false)
            } catch (e: Exception) {
                showNotification("Scan fehlgeschlagen: ${e.message}", isError = true)
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    fun decideMatch(id: Long, keep: Boolean) {
        viewModelScope.launch {
            val thresholdMsg = repository.decideMatch(id, keep)
            if (thresholdMsg != null) {
                showNotification("🎯 $thresholdMsg", isError = false)
            }
        }
    }

    fun clearUndecided() {
        viewModelScope.launch {
            val n = repository.clearUndecided()
            showNotification("$n offene Treffer entfernt. Entscheidungen bleiben gespeichert.", isError = false)
        }
    }

    fun testConnections() {
        if (_isTesting.value) return
        viewModelScope.launch {
            _isTesting.value = true
            _connectionResult.value = null
            try {
                _connectionResult.value = repository.testConnections()
            } catch (e: Exception) {
                _connectionResult.value = "Test fehlgeschlagen: ${e.message}"
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun searchGaps(keyword: String) {
        if (keyword.isBlank() || _gapSearching.value) return
        gapJob = viewModelScope.launch {
            _gapSearching.value = true
            _gapMessage.value = null
            try {
                _gapMessage.value = repository.searchGaps(keyword.trim()) { _gapProgress.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _gapMessage.value = "Suche fehlgeschlagen: ${e.message}"
            } finally {
                _gapSearching.value = false
                _gapProgress.value = null
            }
        }
    }

    fun setGapFilter(filter: String) {
        _gapFilter.value = filter
    }

    fun decideGap(id: Long, status: String) {
        viewModelScope.launch { repository.decideGap(id, status) }
    }

    fun clearOpenGaps() {
        viewModelScope.launch {
            val n = repository.clearOpenGaps()
            _gapMessage.value = "$n offene Angebote entfernt."
        }
    }

    fun updateSettings(newSettings: BotSettingEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            showNotification("Einstellungen gespeichert.", isError = false)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            showNotification("Systemprotokoll geleert.", isError = false)
        }
    }

    fun showNotification(message: String, isError: Boolean = false) {
        if (!isError && !uiState.value.settings.notificationsEnabled) return
        _notificationBanner.value = message
        _isErrorNotification.value = isError
    }

    fun dismissNotification() {
        _notificationBanner.value = null
    }
}
