package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.ListingEntity
import com.example.ui.viewmodel.BotUiState

@Composable
fun ListingsScreen(
    uiState: BotUiState,
    onSelectListing: (ListingEntity) -> Unit,
    onOpenAddListing: () -> Unit,
    onSetFilter: (String) -> Unit,
    onSimulateOurSale: (Long) -> Unit,
    onCloneOffers: (Int) -> Unit = {},
    onDeleteSelected: (List<Long>) -> Unit = {},
    onUpdateSelectedMargin: (List<Long>, String, Double) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchMarginDialog by remember { mutableStateOf(false) }

    val filteredListings = remember(uiState.listings, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.listings
        } else {
            uiState.listings.filter {
                it.myTitle.contains(searchQuery, ignoreCase = true) ||
                it.originalTitle.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.originalSeller.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddListing,
                modifier = Modifier.testTag("fab_add_listing"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Angebot crawlen")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Angebote, Verkäufer oder Kategorien suchen...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Löschen")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("listings_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "ALL" to "Alle",
                    "ACTIVE" to "Aktiv (${uiState.activeCount})",
                    "DEACTIVATED" to "Deaktiviert (Schutz)",
                    "SOLD" to "Verkauft"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = uiState.listingFilter == key,
                        onClick = { onSetFilter(key) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("filter_chip_$key")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = { onCloneOffers(uiState.settings.autoCloneCount.takeIf { it > 0 } ?: 10) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("listings_clone_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "⚡ Top-${if (uiState.settings.autoCloneCount > 0) uiState.settings.autoCloneCount else 10} Resonanz-Angebote automatisch klonen",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // BATCH ACTION BAR WHEN ITEMS ARE SELECTED
            if (selectedIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size} ausgewählt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showBatchMarginDialog = true },
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Icon(imageVector = Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Marge ändern", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    onDeleteSelected(selectedIds.toList())
                                    selectedIds = emptySet()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Löschen", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredListings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Keine passenden Angebote gefunden.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onOpenAddListing) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resonanz-Angebot spiegeln")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedIds.size == filteredListings.size) "Alle abwählen" else "Alle auswählen",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        selectedIds = if (selectedIds.size == filteredListings.size) {
                                            emptySet()
                                        } else {
                                            filteredListings.map { it.id }.toSet()
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    items(filteredListings, key = { "listing_${it.id}" }) { listing ->
                        val isSelected = selectedIds.contains(listing.id)
                        ListingItemCard(
                            listing = listing,
                            isSelected = isSelected,
                            onToggleSelection = {
                                selectedIds = if (isSelected) selectedIds - listing.id else selectedIds + listing.id
                            },
                            onClick = { onSelectListing(listing) },
                            onQuickSale = { onSimulateOurSale(listing.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showBatchMarginDialog) {
        BatchMarginDialog(
            onDismiss = { showBatchMarginDialog = false },
            onConfirm = { type, value ->
                onUpdateSelectedMargin(selectedIds.toList(), type, value)
                selectedIds = emptySet()
                showBatchMarginDialog = false
            }
        )
    }
}

@Composable
fun BatchMarginDialog(
    onDismiss: () -> Unit,
    onConfirm: (marginType: String, marginValue: Double) -> Unit
) {
    var marginType by remember { mutableStateOf("PERCENTAGE") }
    var valueText by remember { mutableStateOf("5.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marge für ausgewählte ändern 📊", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Geben Sie die neue Marge ein, die auf alle ausgewählten gespiegelten Anzeigen angewendet werden soll:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = marginType == "FIXED_EURO",
                        onClick = {
                            marginType = "FIXED_EURO"
                            valueText = "1.00"
                        },
                        label = { Text("€ Fester Wert") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = marginType == "PERCENTAGE",
                        onClick = {
                            marginType = "PERCENTAGE"
                            valueText = "5.0"
                        },
                        label = { Text("% Prozentual") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(if (marginType == "PERCENTAGE") "Prozentwert (%)" else "Euro-Wert (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = valueText.toDoubleOrNull() ?: 5.0
                    onConfirm(marginType, parsed)
                }
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ListingItemCard(
    listing: ListingEntity,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    onQuickSale: () -> Unit
) {
    val statusColor = when (listing.status) {
        "ACTIVE" -> MaterialTheme.colorScheme.primary
        "SOLD" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val statusText = when (listing.status) {
        "ACTIVE" -> "Aktiv (Live-Sync)"
        "SOLD" -> "Verkauft (Bot-Kauf aktiv)"
        "ORIGINAL_DELETED" -> "Original gelöscht (Schutz aktiv)"
        else -> listing.status
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listing.myTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Original: ${listing.originalSeller} • ID: ${listing.myListingId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Original: ${String.format("%.2f", listing.originalPrice)} € + ${String.format("%.2f", listing.originalShippingCost)} € VSK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "➔ Unser Preis: ${String.format("%.2f", listing.myPrice)} € (Gratis Versand)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bearbeitungszeit: ${listing.handlingTimeDays} Werktage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "${listing.soldCount} Verkäufe • ${listing.viewsCount} Beobachter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details & Test-Tools", style = MaterialTheme.typography.labelMedium)
                }

                if (listing.status == "ACTIVE") {
                    Button(
                        onClick = onQuickSale,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_sale_button_${listing.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verkauf testen", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
