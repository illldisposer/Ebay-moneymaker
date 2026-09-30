package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BotSettingEntity
import com.example.ui.viewmodel.BotUiState

@Composable
fun SettingsScreen(
    uiState: BotUiState,
    onSaveSettings: (BotSettingEntity) -> Unit,
    onOpenEbayLogin: () -> Unit,
    onCloneNow: (Int) -> Unit = {}
) {
    var isLiveMode by remember(uiState.settings) { mutableStateOf(uiState.settings.isLiveMode) }
    var isBotActive by remember(uiState.settings) { mutableStateOf(uiState.settings.isBotActive) }
    var marginType by remember(uiState.settings) { mutableStateOf(uiState.settings.marginType) } // "FIXED_EURO" or "PERCENTAGE"
    var marginEuroText by remember(uiState.settings) { mutableStateOf(String.format("%.2f", uiState.settings.marginEuro).replace(",", ".")) }
    var marginPercentText by remember(uiState.settings) { mutableStateOf(String.format("%.1f", uiState.settings.marginPercentage).replace(",", ".")) }
    var autoCloneCount by remember(uiState.settings) { mutableIntStateOf(uiState.settings.autoCloneCount) }
    var autoCloneOnLogin by remember(uiState.settings) { mutableStateOf(uiState.settings.autoCloneOnLogin) }
    var syncInterval by remember(uiState.settings) { mutableFloatStateOf(uiState.settings.syncIntervalSeconds.toFloat()) }
    var connectedAccount by remember(uiState.settings) { mutableStateOf(uiState.settings.connectedAccount) }
    var ebayUsername by remember(uiState.settings) { mutableStateOf(uiState.settings.ebayUsername.ifBlank { connectedAccount }) }
    var ebayPassword by remember { mutableStateOf("••••••••••••") }
    var searchKeywords by remember(uiState.settings) { mutableStateOf(uiState.settings.searchKeywords) }
    var notificationsEnabled by remember(uiState.settings) { mutableStateOf(uiState.settings.notificationsEnabled) }

    // Live calculation for preview
    val sampleBasePrice = 100.00
    val sampleShipping = 5.90
    val currentMarginEuro = marginEuroText.toDoubleOrNull() ?: 1.00
    val currentMarginPercent = marginPercentText.toDoubleOrNull() ?: 5.0

    val previewSellingPrice = if (marginType == "PERCENTAGE") {
        Math.round((sampleBasePrice + sampleShipping) * (1.0 + (currentMarginPercent / 100.0)) * 100.0) / 100.0
    } else {
        Math.round((sampleBasePrice + sampleShipping + currentMarginEuro) * 100.0) / 100.0
    }
    val previewProfit = Math.round((previewSellingPrice - (sampleBasePrice + sampleShipping)) * 100.0) / 100.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Title
        Text(
            text = "Bot-Einstellungen & Automatisierung",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. PROMINENT MODE SWITCH: TEST-MODUS vs. LIVE-MODUS
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isLiveMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLiveMode) Color(0xFFD32F2F) else Color(0xFFF57C00))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiveMode) "🔴 Live-Modus (eBay.de)" else "🧪 Test-Modus (Sandbox)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLiveMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Switch(
                        checked = isLiveMode,
                        onCheckedChange = { isLiveMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.testTag("settings_live_mode_switch")
                    )
                }

                Text(
                    text = if (isLiveMode) {
                        "Achtung: Der Bot ist mit www.ebay.de gekoppelt. Angebote werden direkt real auf eBay.de eingestellt und Verkäufe lösen echte automatisierte Sofort-Käufe aus."
                    } else {
                        "Im Testmodus (Sandbox) werden Spiegelungen, Preisabgleiche und Verkäufe gefahrlos und ohne echte eBay-Gebühren simuliert."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLiveMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // 2. MARGE KONFIGURATION (PROZENT vs. FESTBETRAG)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PriceChange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Marge & Aufschlag einstellen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Wählen Sie, wie der Bot Ihren Verkaufspreis über dem ursprünglichen Gesamtpreis (inkl. Versand) kalkulieren soll:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Margin Type Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = marginType == "FIXED_EURO",
                        onClick = { marginType = "FIXED_EURO" },
                        label = { Text("€ Fester Euro-Wert") },
                        leadingIcon = {
                            if (marginType == "FIXED_EURO") {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("margin_type_fixed_chip")
                    )

                    FilterChip(
                        selected = marginType == "PERCENTAGE",
                        onClick = { marginType = "PERCENTAGE" },
                        label = { Text("% Prozent vom Preis") },
                        leadingIcon = {
                            if (marginType == "PERCENTAGE") {
                                Icon(imageVector = Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("margin_type_percent_chip")
                    )
                }

                // Input fields based on selection
                if (marginType == "FIXED_EURO") {
                    OutlinedTextField(
                        value = marginEuroText,
                        onValueChange = { marginEuroText = it },
                        label = { Text("Feste Marge pro Artikel (€)") },
                        supportingText = { Text("Beispiel: 1.00 € = Immer exakt 1 € Reingewinn") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("margin_euro_input")
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.00, 2.00, 3.50, 5.00).forEach { preset ->
                            OutlinedButton(
                                onClick = { marginEuroText = String.format("%.2f", preset).replace(",", ".") },
                                modifier = Modifier.weight(1f),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Text("+${preset.toInt()} €", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = marginPercentText,
                        onValueChange = { marginPercentText = it },
                        label = { Text("Prozentuale Marge (%)") },
                        supportingText = { Text("Beispiel: 5.0 % = 5 Prozent Aufschlag auf Originalpreis + Versand") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("margin_percent_input")
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3.0, 5.0, 8.0, 10.0).forEach { preset ->
                            OutlinedButton(
                                onClick = { marginPercentText = String.format("%.1f", preset).replace(",", ".") },
                                modifier = Modifier.weight(1f),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Text("${preset.toInt()} %", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Live Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Live-Berechnungsvorschau (Musterartikel):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original: 100,00 € + 5,90 € VSK = 105,90 €",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "➔ Unser Verkaufspreis: ${String.format("%.2f", previewSellingPrice)} €",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "➔ Ihr Reingewinn: +${String.format("%.2f", previewProfit)} € (Kostenloser Versand für Käufer)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 3. REAL EBAY.DE LOGIN & INTEGRATION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "eBay.de Konto & Live-Login",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Geben Sie Ihren Account-Namen und Ihr Passwort ein, um die App direkt mit der originalen Seite ebay.de zu verknüpfen:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = ebayUsername,
                    onValueChange = {
                        ebayUsername = it
                        connectedAccount = it
                    },
                    label = { Text("eBay Account-Name / E-Mail") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ebay_username_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ebayPassword,
                    onValueChange = { ebayPassword = it },
                    label = { Text("eBay Passwort") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ebay_password_input"),
                    singleLine = true
                )

                // Direct In-App Browser Login Button
                Button(
                    onClick = onOpenEbayLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_ebay_web_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auf originaler eBay.de Seite einloggen")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.settings.isEbayLoggedIn) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (uiState.settings.isEbayLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.settings.isEbayLoggedIn) {
                            "Status: Verbunden mit eBay.de als '$ebayUsername'"
                        } else {
                            "Status: Nicht eingeloggt (${if (isLiveMode) "Live-Login erforderlich" else "Test-Modus aktiv"})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.settings.isEbayLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. BOT CORE TIMING & SYNC
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bot Aktivitätsstatus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBotActive) "Hintergrund-Sync & Auto-Kauf aktiv" else "Pausiert",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isBotActive,
                        onCheckedChange = { isBotActive = it },
                        modifier = Modifier.testTag("settings_bot_active_switch")
                    )
                }

                HorizontalDivider()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Live-Abgleich Intervall:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${syncInterval.toInt()} Sekunden",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = syncInterval,
                        onValueChange = { syncInterval = it },
                        valueRange = 10f..300f,
                        steps = 5,
                        modifier = Modifier.testTag("settings_sync_slider")
                    )
                }

                OutlinedTextField(
                    value = searchKeywords,
                    onValueChange = { searchKeywords = it },
                    label = { Text("Suchkriterien & Nischen für Auto-Spiegelung") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }
        }

        // 5. AUTOMATISCHES KLONEN VON RESONANZ-ANGEBOTEN (0 bis 1000)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automatisches Resonanz-Klonen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Beim eBay-Login automatisch klonen",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Klont sofort nach dem Einloggen auf ebay.de Angebote aus den stärksten Resonanzgruppen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = autoCloneOnLogin,
                        onCheckedChange = { autoCloneOnLogin = it }
                    )
                }

                HorizontalDivider()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Anzahl zu klonender Angebote:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$autoCloneCount Artikel (0 - 1000)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = autoCloneCount.toFloat(),
                        onValueChange = { autoCloneCount = it.toInt() },
                        valueRange = 0f..1000f,
                        steps = 99
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0, 10, 25, 50, 100, 250, 500, 1000).forEach { preset ->
                            FilterChip(
                                selected = autoCloneCount == preset,
                                onClick = { autoCloneCount = preset },
                                label = { Text("$preset", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Instant Clone Action Button
                Button(
                    onClick = { onCloneNow(autoCloneCount) },
                    enabled = autoCloneCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instant_clone_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jetzt $autoCloneCount Resonanz-Artikel sofort klonen ⚡")
                }
            }
        }

        // 6. NOTIFICATION SETTINGS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Benachrichtigungen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sofortige Warnung bei Preisänderungen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }
        }

        // SAVE BUTTON
        Button(
            onClick = {
                val parsedEuro = marginEuroText.toDoubleOrNull() ?: 1.00
                val parsedPercent = marginPercentText.toDoubleOrNull() ?: 5.0
                onSaveSettings(
                    uiState.settings.copy(
                        isLiveMode = isLiveMode,
                        isBotActive = isBotActive,
                        marginType = marginType,
                        marginEuro = parsedEuro,
                        marginPercentage = parsedPercent,
                        autoCloneCount = autoCloneCount,
                        autoCloneOnLogin = autoCloneOnLogin,
                        syncIntervalSeconds = syncInterval.toInt(),
                        connectedAccount = ebayUsername.ifBlank { connectedAccount },
                        ebayUsername = ebayUsername,
                        searchKeywords = searchKeywords,
                        notificationsEnabled = notificationsEnabled
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_settings_button")
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Einstellungen & Marge speichern", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
