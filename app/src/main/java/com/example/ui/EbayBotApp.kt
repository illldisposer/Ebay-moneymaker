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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.data.local.entity.ListingEntity
import com.example.data.local.entity.OrderEntity
import com.example.ui.screens.AddListingDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ListingDetailDialog
import com.example.ui.screens.ListingsScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.OrderFulfillmentDialog
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.EbayBotViewModel
import kotlinx.coroutines.delay

enum class ScreenTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    LISTINGS("Angebote", Icons.Default.ListAlt),
    ORDERS("Bestellungen", Icons.Default.ReceiptLong),
    LOGS("Protokoll", Icons.Default.ViewHeadline),
    SETTINGS("Einstellungen", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbayBotApp(viewModel: EbayBotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }

    var selectedListingForDetail by remember { mutableStateOf<ListingEntity?>(null) }
    var selectedOrderForFulfillment by remember { mutableStateOf<OrderEntity?>(null) }
    var showAddListingDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

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
                            imageVector = Icons.Default.Sync,
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
                    IconButton(
                        onClick = { viewModel.performLiveSyncCycle() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.testTag("topbar_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = if (uiState.isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                            if (tab == ScreenTab.ORDERS && uiState.pendingOrderCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("${uiState.pendingOrderCount}") }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Live Notification Banner
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
                    ScreenTab.DASHBOARD -> DashboardScreen(
                        uiState = uiState,
                        onSyncNow = { viewModel.performLiveSyncCycle() },
                        onOpenAddListing = { showAddListingDialog = true },
                        onSelectListing = { selectedListingForDetail = it },
                        onNavigateToOrders = { currentTab = ScreenTab.ORDERS },
                        onNavigateToListings = { currentTab = ScreenTab.LISTINGS },
                        onNavigateToLogs = { currentTab = ScreenTab.LOGS },
                        onToggleBotActive = { active ->
                            viewModel.updateSettings(uiState.settings.copy(isBotActive = active))
                        }
                    )
                    ScreenTab.LISTINGS -> ListingsScreen(
                        uiState = uiState,
                        onSelectListing = { selectedListingForDetail = it },
                        onOpenAddListing = { showAddListingDialog = true },
                        onSetFilter = { viewModel.setListingFilter(it) },
                        onSimulateOurSale = { viewModel.simulateOurSale(it) }
                    )
                    ScreenTab.ORDERS -> OrdersScreen(
                        uiState = uiState,
                        onSelectOrder = { selectedOrderForFulfillment = it },
                        onConfirmPayment = { viewModel.confirmPaymentAndFinalize(it) }
                    )
                    ScreenTab.LOGS -> LogsScreen(
                        uiState = uiState,
                        onSetFilter = { viewModel.setLogFilter(it) },
                        onClearLogs = { viewModel.clearLogs() }
                    )
                    ScreenTab.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onSaveSettings = { viewModel.updateSettings(it) }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddListingDialog) {
        AddListingDialog(
            onDismiss = {
                showAddListingDialog = false
                viewModel.clearAiResult()
            },
            onConfirm = { title, desc, price, shipping, seller, category, img, score, reason ->
                viewModel.crawlAndAddListing(title, desc, price, shipping, seller, category, img, score, reason)
                showAddListingDialog = false
                viewModel.clearAiResult()
            },
            onAnalyzeAi = { title, desc, price, shipping, cat ->
                viewModel.analyzeWithGemini(title, desc, price, shipping, cat)
            },
            isAiAnalyzing = uiState.isAiAnalyzing,
            aiResult = uiState.aiAnalysisResult
        )
    }

    selectedListingForDetail?.let { listing ->
        ListingDetailDialog(
            listing = listing,
            onDismiss = { selectedListingForDetail = null },
            onSimulatePriceChange = { id, p, s ->
                viewModel.simulatePriceChange(id, p, s)
            },
            onSimulateOriginalDeleted = { id, reason ->
                viewModel.simulateOriginalDeletion(id, reason)
            },
            onSimulateOurSale = { id ->
                viewModel.simulateOurSale(id)
            },
            onReactivate = { id ->
                viewModel.reactivateListing(id)
            },
            onDelete = { l ->
                viewModel.deleteListing(l)
            }
        )
    }

    selectedOrderForFulfillment?.let { order ->
        // Keep order state fresh from uiState
        val currentOrder = uiState.orders.find { it.id == order.id } ?: order
        OrderFulfillmentDialog(
            order = currentOrder,
            onDismiss = { selectedOrderForFulfillment = null },
            onConfirmPaymentReceived = { id ->
                viewModel.confirmPaymentAndFinalize(id)
            },
            onCompleteOrder = { id, track ->
                viewModel.markOrderCompleted(id, track)
            }
        )
    }
}
