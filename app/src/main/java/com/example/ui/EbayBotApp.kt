package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.GapScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ReviewScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.EbayBotViewModel
import kotlinx.coroutines.delay

enum class ScreenTab(val title: String, val icon: ImageVector) {
    SCANNER("Scanner", Icons.Default.Radar),
    GAPS("Marktlücken", Icons.Default.Search),
    REVIEW("Prüfen", Icons.AutoMirrored.Filled.FactCheck),
    LOGS("Protokoll", Icons.Default.ViewHeadline),
    SETTINGS("Einstellungen", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbayBotApp(viewModel: EbayBotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gapState by viewModel.gapState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(ScreenTab.SCANNER) }

    // Auto dismiss notification banner after 5 seconds
    LaunchedEffect(uiState.notificationBanner) {
        if (uiState.notificationBanner != null) {
            delay(5000)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = "eBay Resonanz Bot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (uiState.settings.ebayUseSandbox) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("topbar_sandbox_badge")
                        ) {
                            Text(
                                text = "🧪 SANDBOX",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (uiState.isScanning) viewModel.stopScan() else viewModel.startScan() },
                        modifier = Modifier.testTag("topbar_scan_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isScanning) "Scan stoppen" else "Scan starten",
                            tint = if (uiState.isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ScreenTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == ScreenTab.REVIEW && uiState.reviewCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("${uiState.reviewCount}") }
                                    }
                                ) {
                                    Icon(imageVector = tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Notification Banner
                AnimatedVisibility(
                    visible = uiState.notificationBanner != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    uiState.notificationBanner?.let { message ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isErrorNotification)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isErrorNotification) Icons.Default.ErrorOutline else Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (uiState.isErrorNotification) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    color = if (uiState.isErrorNotification)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                IconButton(
                                    onClick = { viewModel.dismissNotification() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Schließen",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Tab Content
                when (currentTab) {
                    ScreenTab.SCANNER -> ScannerScreen(
                        uiState = uiState,
                        onStartScan = { viewModel.startScan() },
                        onStopScan = { viewModel.stopScan() },
                        onRejectMatch = { viewModel.decideMatch(it, keep = false) },
                        onClearResults = { viewModel.clearUndecided() },
                        onOpenSettings = { currentTab = ScreenTab.SETTINGS },
                        onOpenReview = { currentTab = ScreenTab.REVIEW }
                    )
                    ScreenTab.GAPS -> GapScreen(
                        state = gapState,
                        onSearch = { viewModel.searchGaps(it) },
                        onFilter = { viewModel.setGapFilter(it) },
                        onDecide = { id, status -> viewModel.decideGap(id, status) },
                        onClearOpen = { viewModel.clearOpenGaps() }
                    )
                    ScreenTab.REVIEW -> ReviewScreen(
                        uiState = uiState,
                        onKeep = { viewModel.decideMatch(it, keep = true) },
                        onReject = { viewModel.decideMatch(it, keep = false) }
                    )
                    ScreenTab.LOGS -> LogsScreen(
                        uiState = uiState,
                        onSetFilter = { viewModel.setLogFilter(it) },
                        onClearLogs = { viewModel.clearLogs() }
                    )
                    ScreenTab.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onSaveSettings = { viewModel.updateSettings(it) },
                        onTestConnections = { viewModel.testConnections() }
                    )
                }
            }
        }
    }
}
