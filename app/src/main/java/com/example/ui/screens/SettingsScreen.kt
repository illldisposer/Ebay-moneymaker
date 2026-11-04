package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BotSettingEntity
import com.example.ui.viewmodel.BotUiState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: BotUiState,
    onSaveSettings: (BotSettingEntity) -> Unit,
    onTestConnections: () -> Unit
) {
    val s = uiState.settings

    var ebayClientId by remember(s.ebayClientId) { mutableStateOf(s.ebayClientId) }
    var ebayClientSecret by remember(s.ebayClientSecret) { mutableStateOf(s.ebayClientSecret) }
    var ebaySandbox by remember(s.ebayUseSandbox) { mutableStateOf(s.ebayUseSandbox) }
    var aliAppKey by remember(s.aliAppKey) { mutableStateOf(s.aliAppKey) }
    var aliAppSecret by remember(s.aliAppSecret) { mutableStateOf(s.aliAppSecret) }
    var aliTrackingId by remember(s.aliTrackingId) { mutableStateOf(s.aliTrackingId) }
    var keywords by remember(s.searchKeywords) { mutableStateOf(s.searchKeywords) }
    var productsPerKeyword by remember(s.productsPerKeyword) { mutableFloatStateOf(s.productsPerKeyword.toFloat()) }
    var candidates by remember(s.ebayCandidatesPerProduct) { mutableFloatStateOf(s.ebayCandidatesPerProduct.toFloat()) }
    var minConfidence by remember(s.minConfidence) { mutableFloatStateOf(s.minConfidence.toFloat()) }
    var autoConfidence by remember(s.autoAcceptConfidence) { mutableFloatStateOf(s.autoAcceptConfidence.toFloat()) }
    var autoAdjust by remember(s.autoAdjustThreshold) { mutableStateOf(s.autoAdjustThreshold) }
    var notifications by remember(s.notificationsEnabled) { mutableStateOf(s.notificationsEnabled) }

    // Mindest-Sicherheit muss unter der Auto-Schwelle liegen
    LaunchedEffect(minConfidence, autoConfidence) {
        if (autoConfidence < minConfidence + 5f) autoConfidence = (minConfidence + 5f).coerceAtMost(99f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text("Einstellungen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        SettingsSection("eBay Browse API") {
            KeyField("Client ID (App ID)", ebayClientId, secret = false, tag = "ebay_client_id") { ebayClientId = it }
            KeyField("Client Secret (Cert ID)", ebayClientSecret, secret = true, tag = "ebay_client_secret") { ebayClientSecret = it }
            SwitchRow("Sandbox statt Produktion", ebaySandbox) { ebaySandbox = it }
        }

        SettingsSection("AliExpress Affiliate API") {
            KeyField("App Key", aliAppKey, secret = false, tag = "ali_app_key") { aliAppKey = it }
            KeyField("App Secret", aliAppSecret, secret = true, tag = "ali_app_secret") { aliAppSecret = it }
            KeyField("Tracking-ID", aliTrackingId, secret = false, tag = "ali_tracking_id") { aliTrackingId = it }
        }

        SettingsSection("Scan") {
            OutlinedTextField(
                value = keywords,
                onValueChange = { keywords = it },
                label = { Text("Suchbegriffe (mit Komma getrennt)") },
                modifier = Modifier.fillMaxWidth().testTag("search_keywords"),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            SliderRow("AliExpress-Produkte je Begriff", productsPerKeyword, 5f..50f, 8) { productsPerKeyword = it }
            SliderRow("eBay-Kandidaten je Produkt", candidates, 3f..10f, 6) { candidates = it }
        }

        SettingsSection("Zuordnung") {
            SliderRow("Mindest-Sicherheit (darunter verworfen)", minConfidence, 20f..80f, 11, suffix = " %") { minConfidence = it }
            SliderRow("Auto-Schwelle (ab hier sicher)", autoConfidence, 50f..99f, 48, suffix = " %") { autoConfidence = it }
            Text(
                text = "Zwischen beiden Werten landen Treffer in „Prüfen“. Die Zuordnung läuft lokal auf dem Gerät (Titelvergleich + Bild-Fingerabdruck), es gehen keine Daten an Dritte.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SwitchRow("Auto-Schwelle aus Prüf-Entscheidungen lernen", autoAdjust) { autoAdjust = it }
        }

        SettingsSection("App") {
            SwitchRow("Benachrichtigungs-Banner", notifications) { notifications = it }
        }

        Button(
            onClick = {
                onSaveSettings(
                    s.copy(
                        ebayClientId = ebayClientId.trim(),
                        ebayClientSecret = ebayClientSecret.trim(),
                        ebayUseSandbox = ebaySandbox,
                        aliAppKey = aliAppKey.trim(),
                        aliAppSecret = aliAppSecret.trim(),
                        aliTrackingId = aliTrackingId.trim(),
                        searchKeywords = keywords,
                        productsPerKeyword = productsPerKeyword.roundToInt(),
                        ebayCandidatesPerProduct = candidates.roundToInt(),
                        minConfidence = minConfidence.roundToInt(),
                        autoAcceptConfidence = autoConfidence.roundToInt(),
                        autoAdjustThreshold = autoAdjust,
                        notificationsEnabled = notifications
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Speichern")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onTestConnections,
            enabled = !uiState.isTestingConnections,
            modifier = Modifier.fillMaxWidth().testTag("test_connections_button")
        ) {
            if (uiState.isTestingConnections) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("Verbindungen testen (vorher speichern)")
        }

        uiState.connectionTestResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(result, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun KeyField(label: String, value: String, secret: Boolean, tag: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(!secret) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text),
        trailingIcon = if (secret) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Verbergen" else "Anzeigen"
                    )
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String = "",
    onChange: (Float) -> Unit
) {
    Column {
        Text("$label: ${value.roundToInt()}$suffix", style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}
