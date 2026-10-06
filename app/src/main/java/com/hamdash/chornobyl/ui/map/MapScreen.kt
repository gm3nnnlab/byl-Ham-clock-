package com.hamdash.chornobyl.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.FilterChipRow
import com.hamdash.chornobyl.ui.common.RepeaterDetailSheet
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.UiState
import com.hamdash.chornobyl.ui.common.rememberContainer

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val container = rememberContainer()
    val requestPermission = rememberLocationPermissionRequest(container.locationTracker)
    var recenter by remember { mutableIntStateOf(0) }

    val showGps = state.settings.showGps
    val fix = if (showGps) state.location as? LocationState.Fix else null
    val visible = (state.repeaters as? UiState.Content)?.data.orEmpty()

    ScreenScaffold(title = "Map") { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            FilterChipRow(state.filter, viewModel::setFilter, Modifier.padding(vertical = 8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                OsmMap(
                    repeaters = visible,
                    userLocation = fix,
                    modifier = Modifier.fillMaxSize(),
                    recenterRequest = recenter,
                    tileUrl = state.settings.tileUrl,
                    onRepeaterClick = viewModel::select,
                )
                Column(Modifier.align(Alignment.TopCenter).padding(8.dp)) {
                    if (showGps) {
                        LocationStatusBanner(state.location, onRequestPermission = requestPermission)
                    }
                    if (state.repeaters is UiState.Empty) {
                        Surface(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                if (state.total == 0) "No repeaters in the local database yet." else "No ${state.filter.label} repeaters to show.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        when {
                            !showGps -> {
                                viewModel.enableGps()
                                requestPermission()
                            }
                            state.location is LocationState.Fix -> recenter++
                            state.location is LocationState.NoPermission -> requestPermission()
                            else -> Unit
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "My location")
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
