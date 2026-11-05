package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.entity.GapEntity
import com.example.data.local.entity.ebayTotal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.GapUiState
import java.net.URLEncoder

private val gapFilterOptions = listOf(
    "OPEN" to "Offen",
    "GAP" to "Nur eBay",
    "BOTH" to "Auf beiden",
    "UNCLEAR" to "Unklar",
    "ALL" to "Alle"
)

fun gapStatusLabel(status: String): String = when (status) {
    "BOTH" -> "Auf beiden Plattformen"
    "GAP" -> "Nur auf eBay (Lücke)"
    "UNCLEAR" -> "Unklar"
    else -> "Noch nicht geprüft"
}

/** Öffnet die Amazon-Suche im Browser-Fenster. Nur die Suchseite, kein automatisches Auslesen. */
fun amazonSearchUrl(query: String): String =
    "https://www.amazon.de/s?k=" + URLEncoder.encode(query, "UTF-8")

@Composable
fun GapScreen(
    state: GapUiState,
    onSearch: (String) -> Unit,
    onFilter: (String) -> Unit,
    onDecide: (Long, String) -> Unit,
    onClearOpen: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf<GapEntity?>(null) }
    val uriHandler = LocalUriHandler.current

    val visible = if (state.filter == "ALL") state.gaps else state.gaps.filter { it.amazonStatus == state.filter }
    val openCount = state.gaps.count { it.amazonStatus == "OPEN" }
    val gapCount = state.gaps.count { it.amazonStatus == "GAP" }
    val bothCount = state.gaps.count { it.amazonStatus == "BOTH" }
    val unclearCount = state.gaps.count { it.amazonStatus == "UNCLEAR" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("gap_screen")
    ) {
        Text(text = "Marktlücken", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "Gesamt ${state.gaps.size} · offen $openCount · nur eBay $gapCount · auf beiden $bothCount · unklar $unclearCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gap_search_field"),
                placeholder = { Text("z. B. Edelstahl Trinkflasche") },
                singleLine = true,
                enabled = !state.isSearching,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(keyword) })
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onSearch(keyword) },
                enabled = !state.isSearching && keyword.isNotBlank(),
                modifier = Modifier.testTag("gap_search_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Auf eBay suchen")
            }
        }

        if (state.isSearching) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.progress?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall)
            }
        }
        state.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            items(gapFilterOptions) { (key, label) ->
                FilterChip(
                    selected = state.filter == key,
                    onClick = { onFilter(key) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("gap_filter_$key")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${visible.size} Angebote", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onClearOpen, enabled = openCount > 0) {
                Text("Offene entfernen")
            }
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.gaps.isEmpty()) {
                        "Suchbegriff eingeben und auf die Lupe tippen."
                    } else {
                        "Keine Angebote in diesem Filter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(visible, key = { it.id }) { gap ->
                    GapCard(
                        gap = gap,
                        onCheck = { checking = gap },
                        onOpenEbay = { uriHandler.openUri(gap.ebayItemUrl) }
                    )
                }
            }
        }
    }

    checking?.let { gap ->
        AmazonCheckDialog(
            gap = gap,
            onDecide = { status ->
                onDecide(gap.id, status)
                checking = null
            },
            onClose = { checking = null }
        )
    }
}

@Composable
private fun GapCard(
    gap: GapEntity,
    onCheck: () -> Unit,
    onOpenEbay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("gap_card_${gap.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = gap.ebayImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gap.ebayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "eBay ${formatEuro(gap.ebayTotal())} inkl. Versand",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = gap.ebaySoldEstimate?.let { "Verkaufsschätzung laut eBay: ca. $it" }
                        ?: "Keine Verkaufsangabe von eBay",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = gapStatusLabel(gap.amazonStatus),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (gap.amazonStatus == "GAP") SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    Button(
                        onClick = onCheck,
                        modifier = Modifier.testTag("gap_check_${gap.id}")
                    ) {
                        Text("Auf Amazon prüfen")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = onOpenEbay) {
                        Text("eBay")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmazonCheckDialog(
    gap: GapEntity,
    onDecide: (String) -> Unit,
    onClose: () -> Unit
) {
    var query by remember(gap.id) { mutableStateOf(gap.amazonQuery) }
    var searchUrl by remember(gap.id) { mutableStateOf(amazonSearchUrl(gap.amazonQuery)) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Amazon-Prüfung", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    }
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "eBay: ${gap.ebayTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Amazon-Suchbegriff") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { searchUrl = amazonSearchUrl(query) }) {
                            Text("Suchen")
                        }
                    }
                }
                AndroidView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            tag = searchUrl
                            loadUrl(searchUrl)
                        }
                    },
                    update = { view ->
                        if (view.tag != searchUrl) {
                            view.tag = searchUrl
                            view.loadUrl(searchUrl)
                        }
                    }
                )
                Text(
                    text = "Vergleiche Modell und Menge. Bei Zubehör oder anderer Variante lieber „Unklar“ wählen.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onDecide("BOTH") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gap_decide_both")
                    ) {
                        Text("Auf Amazon")
                    }
                    OutlinedButton(
                        onClick = { onDecide("GAP") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gap_decide_gap")
                    ) {
                        Text("Lücke")
                    }
                    OutlinedButton(
                        onClick = { onDecide("UNCLEAR") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gap_decide_unclear")
                    ) {
                        Text("Unklar")
                    }
                }
            }
        }
    }
}
