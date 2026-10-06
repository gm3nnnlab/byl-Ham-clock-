package com.hamdash.chornobyl.ui.repeaters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.domain.formatDistance
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.domain.formatOffset
import com.hamdash.chornobyl.domain.haversineKm
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.ErrorView
import com.hamdash.chornobyl.ui.common.FilterChipRow
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.ModeChip
import com.hamdash.chornobyl.ui.common.RepeaterDetailSheet
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.StatusChip
import com.hamdash.chornobyl.ui.common.UiState
import com.hamdash.chornobyl.ui.common.syncSummary
import com.hamdash.chornobyl.ui.theme.NumberFont

@Composable
fun RepeatersScreen(viewModel: RepeatersViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = remember(state.settings.timeZoneId) { TimeZoneOptions.resolve(state.settings.timeZoneId) }
    val showGps = state.settings.showGps
    val fix = if (showGps) state.location as? LocationState.Fix else null

    ScreenScaffold(
        title = "Radio Repeaters",
        actions = { StatusChip(online = state.status.online) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                syncSummary(state.status, zone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            state.message?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissMessage) { Text("OK") }
                    }
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                label = { Text("Search callsign, name or frequency") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FilterChipRow(state.filter, viewModel::setFilter, Modifier.padding(bottom = 8.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val content = state.content) {
                    UiState.Loading -> LoadingView()
                    is UiState.Error -> ErrorView(content.message, onRetry = viewModel::syncNow)
                    UiState.Empty -> if (state.total == 0) {
                        EmptyView(
                            title = "No repeaters yet",
                            message = "The local database is empty. Sync to load the bundled data or your data source.",
                            actionLabel = "Sync now",
                            onAction = viewModel::syncNow,
                        )
                    } else {
                        EmptyView(
                            title = "No matches",
                            message = "No repeaters match the current filter or search. Try All or clear the search field.",
                        )
                    }
                    is UiState.Content -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(content.data, key = { it.id }) { repeater ->
                            RepeaterRow(
                                repeater = repeater,
                                distance = fix?.let {
                                    formatDistance(
                                        haversineKm(it.latitude, it.longitude, repeater.latitude, repeater.longitude),
                                        state.settings.distanceUnit,
                                    )
                                },
                                isFavorite = repeater.id in state.favoriteIds,
                                onToggleFavorite = { viewModel.toggleFavorite(repeater) },
                                onClick = { viewModel.select(repeater) },
                            )
                        }
                    }
                }
            }
        }
    }

    state.selected?.let { repeater ->
        RepeaterDetailSheet(
            repeater = repeater,
            location = if (showGps) state.location else LocationState.Disabled,
            unit = state.settings.distanceUnit,
            isFavorite = repeater.id in state.favoriteIds,
            onToggleFavorite = { viewModel.toggleFavorite(repeater) },
            onDismiss = { viewModel.select(null) },
        )
    }
}

@Composable
private fun RepeaterRow(
    repeater: Repeater,
    distance: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(repeater.callsign, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    ModeChip(repeater.mode)
                    repeater.bandName?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(repeater.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "RX ${formatMHz(repeater.rxMHz)}  TX ${formatMHz(repeater.txMHz)}  ${formatOffset(repeater.offsetMHz)}",
                    fontFamily = NumberFont,
                    style = MaterialTheme.typography.bodySmall,
                )
                val extras = buildList {
                    repeater.ctcssHz?.let { add("CTCSS ${if (it % 1.0 == 0.0) it.toInt() else it} Hz") }
                    if (repeater.mode == RadioMode.DMR) {
                        repeater.colorCode?.let { add("CC $it") }
                        repeater.timeslot?.let { add("TS $it") }
                    }
                    distance?.let { add(it) }
                }
                if (extras.isNotEmpty()) {
                    Text(extras.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
