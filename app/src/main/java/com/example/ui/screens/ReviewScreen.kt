package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.bestAliLink
import com.example.data.local.entity.ebayTotal
import com.example.data.local.entity.priceDiff
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.BotUiState

@Composable
fun ReviewScreen(
    uiState: BotUiState,
    onKeep: (Long) -> Unit,
    onReject: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text("Prüfen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "${uiState.reviewQueue.size} offen · Auto-Schwelle ${uiState.settings.autoAcceptConfidence} %" +
                if (uiState.settings.autoAdjustThreshold) " (lernt aus deinen Entscheidungen)" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.reviewQueue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 60.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "Nichts zu prüfen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(uiState.reviewQueue, key = { "review_${it.id}" }) { match ->
                    ReviewCard(
                        match = match,
                        onKeep = { onKeep(match.id) },
                        onReject = { onReject(match.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
fun ReviewCard(match: MatchEntity, onKeep: () -> Unit, onReject: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val diff = match.priceDiff()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReviewSide(
                    modifier = Modifier.weight(1f),
                    label = "AliExpress",
                    imageUrl = match.aliImageUrl,
                    title = match.aliTitle,
                    price = formatEuro(match.aliPrice),
                    extra = if (match.aliSalesVolume > 0) "${match.aliSalesVolume} verkauft" else "",
                    onOpen = { runCatching { uriHandler.openUri(match.bestAliLink()) } }
                )
                ReviewSide(
                    modifier = Modifier.weight(1f),
                    label = "eBay",
                    imageUrl = match.ebayImageUrl,
                    title = match.ebayTitle,
                    price = formatEuro(match.ebayTotal()) + " inkl. Versand",
                    extra = match.ebaySeller,
                    onOpen = { runCatching { uriHandler.openUri(match.ebayItemUrl) } }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Differenz: ${if (diff >= 0) "+" else ""}${formatEuro(diff)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (diff > 0) SuccessGreen else DangerRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            ConfidenceChip(match = match)

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).testTag("review_reject_${match.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raus")
                }
                Button(
                    onClick = onKeep,
                    modifier = Modifier.weight(1f).testTag("review_keep_${match.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Behalten")
                }
            }
        }
    }
}

@Composable
private fun ReviewSide(
    modifier: Modifier,
    label: String,
    imageUrl: String,
    title: String,
    price: String,
    extra: String,
    onOpen: () -> Unit
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        AsyncImage(
            model = imageUrl,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onOpen() }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text(price, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        if (extra.isNotBlank()) {
            Text(extra, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
