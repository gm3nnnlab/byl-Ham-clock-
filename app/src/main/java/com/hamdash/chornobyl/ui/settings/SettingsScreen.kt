package com.hamdash.chornobyl.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.settings.ThemeMode
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.syncSummary

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    var confirmClear by remember { mutableStateOf(false) }
    var urlDraft by remember(settings.catalogUrl) { mutableStateOf(settings.catalogUrl) }
    var tileDraft by remember(settings.tileUrl) { mutableStateOf(settings.tileUrl) }

    ScreenScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { message ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissMessage) { Text("OK") }
                    }
                }
            }

            SectionCard("DISPLAY") {
                ChoiceSetting("Theme", settings.themeMode, ThemeMode.entries, { it.label }, viewModel::setTheme)
                ChoiceSetting(
                    "Local time zone",
                    settings.timeZoneId,
                    TimeZoneOptions.ids,
                    { TimeZoneOptions.label(it) },
                    viewModel::setTimeZone,
                )
                SwitchSetting("Show UTC clock", "Display UTC next to local time on the dashboard.", settings.showUtc, viewModel::setShowUtc)
                ChoiceSetting("Distance units", settings.distanceUnit, DistanceUnit.entries, { it.label }, viewModel::setDistanceUnit)
            }

            SectionCard("LOCATION") {
                SwitchSetting(
                    "Show my location",
                    "Uses GPS only to show your position on the map and distances to repeaters. It never leaves your device.",
                    settings.showGps,
                    viewModel::setShowGps,
                )
            }

            SectionCard("DATA AND SYNC") {
                Text(
                    syncSummary(state.status, TimeZoneOptions.resolve(settings.timeZoneId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.status.sync.lastError?.let {
                    Text("Last problem: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                SwitchSetting("Automatic sync", "Refresh the database when the app starts and the interval has passed.", settings.autoSync, viewModel::setAutoSync)
                ChoiceSetting(
                    "Update interval",
                    settings.syncIntervalHours,
                    AppSettings.SYNC_INTERVAL_OPTIONS,
                    { intervalLabel(it) },
                    viewModel::setSyncInterval,
                )
                SwitchSetting("Use mobile data", "When off, the database and propagation only sync on unmetered networks such as Wi-Fi.", settings.allowMobileData, viewModel::setAllowMobileData)

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    label = { Text("Data source URL (https, optional)") },
                    supportingText = { Text("A catalog JSON in the same format as the bundled one. Leave empty to use bundled data only.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.saveCatalogUrl(urlDraft) }) { Text("Save URL") }
                    Button(onClick = viewModel::syncNow, enabled = !state.busy) { Text(if (state.busy) "Working…" else "Sync now") }
                }
            }

            SectionCard("MAP TILES") {
                Text(
                    "By default the map uses the public OpenStreetMap tile server, which is meant for light use. " +
                        "For heavy use, enter your own or a commercial tile server that allows it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = tileDraft,
                    onValueChange = { tileDraft = it },
                    label = { Text("Tile URL (optional)") },
                    supportingText = { Text("Example: https://tiles.example.org/{z}/{x}/{y}.png. Leave empty for the default.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedButton(onClick = { viewModel.saveTileUrl(tileDraft) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Save tile source")
                }
            }

            SectionCard("STORAGE") {
                Text(
                    "Clearing the cache removes downloaded repeater data, cached propagation values and saved map tiles. " +
                        "The bundled data is restored. Favorites and settings are kept.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { confirmClear = true }, enabled = !state.busy, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Clear cache")
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear cache?") },
            text = { Text("Downloaded data and saved map tiles will be removed. The bundled data will be restored.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearCache()
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

private fun intervalLabel(hours: Int): String = when (hours) {
    1 -> "Every hour"
    24 -> "Every day"
    72 -> "Every 3 days"
    else -> "Every $hours hours"
}

@Composable
private fun SwitchSetting(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> ChoiceSetting(title: String, value: T, options: List<T>, label: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(label(value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                onSelect(option)
                                open = false
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == value, onClick = {
                                onSelect(option)
                                open = false
                            })
                            Text(label(option))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
        )
    }
}

