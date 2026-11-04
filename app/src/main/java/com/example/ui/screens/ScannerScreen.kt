package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.api.EbayItem
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.bestAliLink
import com.example.data.local.entity.ebayTotal
import com.example.data.local.entity.priceDiff
import com.example.data.local.entity.priceDiffPercent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.BotUiState
import com.example.ui.viewmodel.EbayTestState
import java.util.Locale

fun formatEuro(value: Double): String = String.format(Locale.GERMANY, "%.2f €", value)

@Composable
fun ScannerScreen(
    uiState: BotUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onRejectMatch: (Long) -> Unit,
    onClearResults: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    ebayTest: EbayTestState = EbayTestState(),
    onRunEbayTest: (String) -> Unit = {},
    onClearEbayTest: () -> Unit = {}
) {
    val settings = uiState.settings
    val keysMissing = !settings.hasEbayKeys() || !settings.hasAliKeys()
    var showClearConfirm by remember { mutableStateOf(false) }
    var ebayQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Preis-Scanner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${uiState.matches.size} Treffer · Auto-Schwelle ${settings.autoAcceptConfidence} %",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.matches.isNotEmpty() || uiState.reviewCount > 0) {
                OutlinedButton(onClick = { showClearConfirm = true }, enabled = !uiState.isScanning) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Leeren", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (keysMissing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "eBay- oder AliExpress-Schlüssel fehlen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenSettings) { Text("Einstellungen") }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Suchbegriffe: ${settings.keywordList().joinToString(", ").ifBlank { "–" }}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isScanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.scanProgress ?: "Läuft …",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = onStopScan, modifier = Modifier.fillMaxWidth().testTag("scan_stop_button")) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stopp")
                    }
                } else {
                    Button(
                        onClick = onStartScan,
                        enabled = !keysMissing,
                        modifier = Modifier.fillMaxWidth().testTag("scan_start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan starten")
                    }
                }
                if (uiState.reviewCount > 0) {
                    TextButton(onClick = onOpenReview, modifier = Modifier.fillMaxWidth()) {
                        Text("${uiState.reviewCount} unsichere Treffer warten in „Prüfen“")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = ebayQuery,
                    onValueChange = { ebayQuery = it },
                    label = { Text("Suchbegriff für eBay-Test") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onRunEbayTest(ebayQuery) },
                        enabled = settings.hasEbayKeys() && ebayQuery.isNotBlank() && !ebayTest.isRunning,
                        modifier = Modifier.weight(1f).testTag("ebay_test_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (ebayTest.isRunning) "Sucht …" else "Nur eBay testen")
                    }
                    if (ebayTest.items.isNotEmpty() || ebayTest.error != null) {
                        TextButton(onClick = onClearEbayTest) { Text("Schließen") }
                    }
                }
                if (ebayTest.isRunning) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                ebayTest.error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (ebayTest.items.isNotEmpty()) {
            Text(
                text = "eBay-Test „${ebayTest.query}“: ${ebayTest.items.size} Treffer",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(ebayTest.items) { item ->
                    EbayTestCard(item = item)
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        } else if (uiState.matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 60.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "Noch keine sicheren Treffer. Starte einen Scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(uiState.matches, key = { "match_${it.id}" }) { match ->
                    MatchCard(match = match, onReject = { onRejectMatch(match.id) })
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Ergebnisse leeren?") },
            text = { Text("Entfernt alle offenen Treffer (Scanner und Prüfen). Deine Entscheidungen „Behalten“ und „Raus“ bleiben gespeichert.") },
            confirmButton = {
                Button(onClick = {
                    onClearResults()
                    showClearConfirm = false
                }) { Text("Leeren") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
fun MatchCard(match: MatchEntity, onReject: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val diff = match.priceDiff()
    val diffColor = if (diff > 0) SuccessGreen else DangerRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumb(url = match.aliImageUrl, label = "Ali")
                Spacer(modifier = Modifier.width(6.dp))
                ProductThumb(url = match.ebayImageUrl, label = "eBay")
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.aliTitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (diff >= 0) "+" else ""}${formatEuro(diff)}  (${String.format(Locale.GERMANY, "%.0f", match.priceDiffPercent())} %)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = diffColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("AliExpress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatEuro(match.aliPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (match.aliSalesVolume > 0) {
                        Text("${match.aliSalesVolume} verkauft", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("eBay inkl. Versand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatEuro(match.ebayTotal()), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${match.ebayCandidateCount} Angebote gefunden", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            ConfidenceChip(match = match)

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(match.bestAliLink()) } },
                    modifier = Modifier.weight(1f)
                ) { Text("AliExpress", style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(match.ebayItemUrl) } },
                    modifier = Modifier.weight(1f)
                ) { Text("eBay", style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Falsch", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
fun EbayTestCard(item: EbayItem) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(item.itemUrl) } },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductThumb(url = item.imageUrl, label = "eBay", sizeDp = 56)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatEuro(item.price + item.shipping),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Preis ${formatEuro(item.price)} + Versand ${formatEuro(item.shipping)}" +
                        if (item.condition.isNotBlank()) " · ${item.condition}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProductThumb(url: String, label: String, sizeDp: Int = 64) {
    Box {
        AsyncImage(
            model = url,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(topEnd = 6.dp, bottomStart = 8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun ConfidenceChip(match: MatchEntity) {
    val color = when {
        match.confidence >= 85 -> SuccessGreen
        match.confidence >= 60 -> MaterialTheme.colorScheme.tertiary
        else -> DangerRed
    }
    Column {
        Text(
            text = "Sicherheit ${match.confidence} % · ${match.matchMethod}" + if (match.status == "KEPT") " · von dir bestätigt" else "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        if (match.matchReason.isNotBlank()) {
            Text(
                text = match.matchReason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
