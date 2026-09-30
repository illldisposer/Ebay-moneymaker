package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.gemini.ResonanceAnalysisResult
import com.example.data.model.ResonanceCatalog
import com.example.data.model.ResonanceOfferTemplate

@Composable
fun AddListingDialog(
    defaultMarginType: String = "FIXED_EURO",
    defaultMarginEuro: Double = 1.00,
    defaultMarginPercent: Double = 5.0,
    isLiveMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        price: Double,
        shipping: Double,
        seller: String,
        category: String,
        imageUrl: String,
        resonanceScore: Int,
        resonanceReason: String,
        customSellingPrice: Double
    ) -> Unit,
    onAnalyzeAi: (title: String, description: String, price: Double, shipping: Double, category: String) -> Unit,
    isAiAnalyzing: Boolean,
    aiResult: ResonanceAnalysisResult?
) {
    val initialOffer = remember { ResonanceCatalog.getRandomOffer() }
    var currentOffer by remember { mutableStateOf(initialOffer) }

    var title by remember { mutableStateOf(currentOffer.title) }
    var description by remember { mutableStateOf(currentOffer.description) }
    var priceText by remember { mutableStateOf(String.format("%.2f", currentOffer.originalPrice).replace(",", ".")) }
    var shippingText by remember { mutableStateOf(String.format("%.2f", currentOffer.originalShippingCost).replace(",", ".")) }
    var seller by remember { mutableStateOf(currentOffer.originalSeller) }
    var category by remember { mutableStateOf(currentOffer.category) }
    var imageUrl by remember { mutableStateOf(currentOffer.imageUrl) }
    var currentScore by remember { mutableIntStateOf(currentOffer.resonanceScore) }
    var currentReason by remember { mutableStateOf(currentOffer.resonanceReason) }

    fun applyTemplate(t: ResonanceOfferTemplate) {
        currentOffer = t
        title = t.title
        description = t.description
        priceText = String.format("%.2f", t.originalPrice).replace(",", ".")
        shippingText = String.format("%.2f", t.originalShippingCost).replace(",", ".")
        seller = t.originalSeller
        category = t.category
        imageUrl = t.imageUrl
        currentScore = t.resonanceScore
        currentReason = t.resonanceReason
    }

    // Dynamic margin settings in dialog
    var marginType by remember { mutableStateOf(defaultMarginType) }
    var marginValueText by remember {
        mutableStateOf(
            if (defaultMarginType == "PERCENTAGE") String.format("%.1f", defaultMarginPercent).replace(",", ".")
            else String.format("%.2f", defaultMarginEuro).replace(",", ".")
        )
    }

    val originalPrice = priceText.toDoubleOrNull() ?: 0.0
    val shipping = shippingText.toDoubleOrNull() ?: 0.0
    val marginValue = marginValueText.toDoubleOrNull() ?: 1.00

    val baseTotal = originalPrice + shipping
    val myCalculatedPrice = if (marginType == "PERCENTAGE") {
        Math.round(baseTotal * (1.0 + (marginValue / 100.0)) * 100.0) / 100.0
    } else {
        Math.round((baseTotal + marginValue) * 100.0) / 100.0
    }
    val calculatedMarginEuro = Math.round((myCalculatedPrice - baseTotal) * 100.0) / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Resonanz-Angebot spiegeln",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLiveMode) Color(0xFFD32F2F) else Color(0xFFF57C00))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isLiveMode) "LIVE" else "TEST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shuffle / Switch Offer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vorschläge aus Top-Resonanzgruppen:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = {
                            val next = (ResonanceCatalog.TOP_OFFERS - currentOffer).randomOrNull() ?: ResonanceCatalog.getRandomOffer()
                            applyTemplate(next)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("shuffle_resonance_offer_button")
                    ) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vorschlag wechseln 🎲", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Quick selector horizontal chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ResonanceCatalog.TOP_OFFERS.take(8).forEach { offer ->
                        val isSelected = currentOffer.id == offer.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { applyTemplate(offer) },
                            label = {
                                Text(
                                    text = offer.title.split(" ").take(2).joinToString(" "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Original Angebotstitel (eBay)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dialog_title_input"),
                    singleLine = false,
                    maxLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Sofort-Kauf (€)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_dialog_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shippingText,
                        onValueChange = { shippingText = it },
                        label = { Text("Versand VSK (€)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_dialog_shipping_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seller,
                        onValueChange = { seller = it },
                        label = { Text("Original Verkäufer") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategorie") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // MARGEN-WAHL DIREKT IM DIALOG
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Marge anpassen:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = marginType == "FIXED_EURO",
                                onClick = {
                                    marginType = "FIXED_EURO"
                                    marginValueText = "1.00"
                                },
                                label = { Text("€ Fester Wert") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = marginType == "PERCENTAGE",
                                onClick = {
                                    marginType = "PERCENTAGE"
                                    marginValueText = "5.0"
                                },
                                label = { Text("% Prozentual") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = marginValueText,
                            onValueChange = { marginValueText = it },
                            label = { Text(if (marginType == "PERCENTAGE") "Aufschlag in Prozent (%)" else "Aufschlag in Euro (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Calculation Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Automatische Preisberechnung:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original: ${String.format("%.2f", originalPrice)} € + ${String.format("%.2f", shipping)} € VSK = ${String.format("%.2f", baseTotal)} €",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "➔ Unser Preis: ${String.format("%.2f", myCalculatedPrice)} € (Kostenloser Versand, ${if (marginType == "PERCENTAGE") "+$marginValueText% = +${String.format("%.2f", calculatedMarginEuro)} €" else "+${String.format("%.2f", marginValue)} €"} Marge)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Bearbeitungszeit: 2 Werktage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // AI resonance button & result
                OutlinedButton(
                    onClick = {
                        onAnalyzeAi(title, description, originalPrice, shipping, category)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_resonance_check_button"),
                    enabled = !isAiAnalyzing
                ) {
                    if (isAiAnalyzing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .height(16.dp)
                                .width(16.dp)
                                .padding(end = 8.dp)
                        )
                        Text("Analysiere Resonanz mit Gemini...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text("KI Resonanz & Marge prüfen")
                    }
                }

                if (aiResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Resonanz-Score: ${aiResult.score}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = aiResult.demandLevel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiResult.analysisText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = aiResult?.score ?: currentScore
                    val reason = aiResult?.analysisText ?: currentReason
                    onConfirm(
                        title,
                        description,
                        originalPrice,
                        shipping,
                        seller,
                        category,
                        imageUrl,
                        score,
                        reason,
                        myCalculatedPrice
                    )
                },
                modifier = Modifier.testTag("confirm_add_listing_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Jetzt einstellen & spiegeln")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Abbrechen")
            }
        }
    )
}
