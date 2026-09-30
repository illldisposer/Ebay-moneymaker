package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.gemini.ResonanceAnalysisResult

@Composable
fun AddListingDialog(
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
        resonanceReason: String
    ) -> Unit,
    onAnalyzeAi: (title: String, description: String, price: Double, shipping: Double, category: String) -> Unit,
    isAiAnalyzing: Boolean,
    aiResult: ResonanceAnalysisResult?
) {
    var title by remember { mutableStateOf("Makita 18V Akku-Kombihammer DHR243Z Brushless") }
    var description by remember { mutableStateOf("Originalverpackter Makita Akku-Kombihammer mit bürstenlosem Motor. Sofort-Kauf Angebot. Neuware.") }
    var priceText by remember { mutableStateOf("149.00") }
    var shippingText by remember { mutableStateOf("6.90") }
    var seller by remember { mutableStateOf("werkzeug_direkt_de") }
    var category by remember { mutableStateOf("Heimwerker & Werkzeuge") }
    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=600&q=80") }

    val originalPrice = priceText.toDoubleOrNull() ?: 0.0
    val shipping = shippingText.toDoubleOrNull() ?: 0.0
    val myCalculatedPrice = Math.round((originalPrice + shipping + 1.00) * 100.0) / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Der Bot ermittelt automatisch Sofort-Kauf Angebote mit hoher Verkaufsresonanz zzgl. Versand und spiegelt diese mit +1,00 € Marge, kostenlosem Versand und 2 Tagen Bearbeitungszeit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shippingText,
                        onValueChange = { shippingText = it },
                        label = { Text("Versand VSK (€)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_dialog_shipping_input"),
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
                            text = "Original: ${String.format("%.2f", originalPrice)} € + ${String.format("%.2f", shipping)} € VSK = ${String.format("%.2f", originalPrice + shipping)} €",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "➔ Unser Preis: ${String.format("%.2f", myCalculatedPrice)} € (Kostenloser Versand, +1,00 € Marge)",
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
                    val score = aiResult?.score ?: 92
                    val reason = aiResult?.analysisText ?: "Hohe Nachfrage und optimale Marge durch kostenlosen Versand."
                    onConfirm(
                        title,
                        description,
                        originalPrice,
                        shipping,
                        seller,
                        category,
                        imageUrl,
                        score,
                        reason
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
