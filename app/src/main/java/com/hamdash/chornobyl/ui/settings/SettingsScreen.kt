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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.settings.AppLanguage
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.settings.ThemeMode
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.asString
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.intervalLabel
import com.hamdash.chornobyl.ui.common.timeZoneLabel
import com.hamdash.chornobyl.ui.common.toUiText
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.syncSummary

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    var confirmClear by remember { mutableStateOf(false) }
    var urlDraft by remember(settings.catalogUrl) { mutableStateOf(settings.catalogUrl) }
    var tileDraft by remember(settings.tileUrl) { mutableStateOf(settings.tileUrl) }

    ScreenScaffold(title = stringResource(R.string.set_title), onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { messageText ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(messageText.asString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.action_ok)) }
                    }
                }
            }

            SectionCard(stringResource(R.string.set_sec_display)) {
                ChoiceSetting(
                    stringResource(R.string.set_language),
                    settings.language,
                    AppLanguage.entries,
                    { it.nativeName },
                    viewModel::setLanguage,
                )
                ChoiceSetting(stringResource(R.string.set_theme), settings.themeMode, ThemeMode.entries, { it.displayName() }, viewModel::setTheme)
                ChoiceSetting(
                    stringResource(R.string.set_timezone),
                    settings.timeZoneId,
                    TimeZoneOptions.ids,
                    { timeZoneLabel(it) },
                    viewModel::setTimeZone,
                )
                SwitchSetting(
                    stringResource(R.string.set_show_utc),
                    stringResource(R.string.set_show_utc_sub),
                    settings.showUtc,
                    viewModel::setShowUtc,
                )
                ChoiceSetting(
                    stringResource(R.string.set_units),
                    settings.distanceUnit,
                    DistanceUnit.entries,
                    { it.displayName() },
                    viewModel::setDistanceUnit,
                )
            }

            SectionCard(stringResource(R.string.set_sec_location)) {
                SwitchSetting(
                    stringResource(R.string.set_show_location),
                    stringResource(R.string.set_show_location_sub),
                    settings.showGps,
                    viewModel::setShowGps,
                )
            }

            SectionCard(stringResource(R.string.set_sec_data)) {
                Text(
                    syncSummary(state.status, TimeZoneOptions.resolve(settings.timeZoneId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.status.sync.lastError?.let {
                    Text(stringResource(R.string.set_last_problem, it.toUiText().asString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                SwitchSetting(
                    stringResource(R.string.set_auto_sync),
                    stringResource(R.string.set_auto_sync_sub),
                    settings.autoSync,
                    viewModel::setAutoSync,
                )
                ChoiceSetting(
                    stringResource(R.string.set_interval),
                    settings.syncIntervalHours,
                    AppSettings.SYNC_INTERVAL_OPTIONS,
                    { intervalLabel(it) },
                    viewModel::setSyncInterval,
                )
                SwitchSetting(
                    stringResource(R.string.set_mobile),
                    stringResource(R.string.set_mobile_sub),
                    settings.allowMobileData,
                    viewModel::setAllowMobileData,
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    label = { Text(stringResource(R.string.set_url_label)) },
                    supportingText = { Text(stringResource(R.string.set_url_support)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.saveCatalogUrl(urlDraft) }) { Text(stringResource(R.string.action_save_url)) }
                    Button(onClick = viewModel::syncNow, enabled = !state.busy) {
                        Text(stringResource(if (state.busy) R.string.action_working else R.string.action_sync_now))
                    }
                }
            }

            SectionCard(stringResource(R.string.set_sec_tiles)) {
                Text(
                    stringResource(R.string.set_tiles_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = tileDraft,
                    onValueChange = { tileDraft = it },
                    label = { Text(stringResource(R.string.set_tile_label)) },
                    supportingText = { Text(stringResource(R.string.set_tile_support)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedButton(onClick = { viewModel.saveTileUrl(tileDraft) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.action_save_tile))
                }
            }

            SectionCard(stringResource(R.string.set_sec_storage)) {
                Text(
                    stringResource(R.string.set_storage_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { confirmClear = true }, enabled = !state.busy, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.action_clear_cache))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.set_clear_title)) },
            text = { Text(stringResource(R.string.set_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearCache()
                }) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
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
private fun <T> ChoiceSetting(
    title: String,
    value: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
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
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

