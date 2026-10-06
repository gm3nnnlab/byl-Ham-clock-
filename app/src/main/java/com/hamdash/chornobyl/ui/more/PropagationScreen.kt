package com.hamdash.chornobyl.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.geomagneticState
import com.hamdash.chornobyl.network.PropagationSources
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.NO_DATA
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.ValueTile
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun PropagationScreen(onBack: () -> Unit, viewModel: PropagationViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val data = snapshot.data

    ScreenScaffold(
        title = "Propagation",
        onBack = onBack,
        actions = {
            if (snapshot.loading) {
                CircularProgressIndicator(Modifier.padding(end = 16.dp).size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = viewModel::refresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("SPACE WEATHER") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueTile("Solar flux (SFI)", data?.solarFlux?.toString() ?: NO_DATA, Modifier.weight(1f))
                    ValueTile("K-index", data?.kIndex?.let { String.format(Locale.US, "%.2f", it) } ?: NO_DATA, Modifier.weight(1f))
                    ValueTile("A-index", data?.aIndex?.toString() ?: NO_DATA, Modifier.weight(1f))
                }
            }
            SectionCard("CONDITIONS") {
                LabeledRow("Geomagnetic", geomagneticState(data?.kIndex)?.label ?: NO_DATA)
                LabeledRow("Last X-ray flare", data?.flareClass ?: NO_DATA)
                data?.flareTime?.let { LabeledRow("Flare time", it) }
                data?.kIndexTime?.let { LabeledRow("K-index time", "$it UTC") }
                LabeledRow(
                    "Fetched",
                    data?.takeIf { it.fetchedAtMillis > 0 }
                        ?.let { TimeFormats.dateTime(Instant.ofEpochMilli(it.fetchedAtMillis), ZoneId.of("UTC")) + " UTC" }
                        ?: NO_DATA,
                )
            }
            snapshot.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard("ABOUT THESE VALUES") {
                Text(
                    "Values come from ${PropagationSources.ATTRIBUTION}. A value shown as NO DATA was not available or could not be " +
                        "read; the app never estimates it. Higher solar flux generally helps the higher HF bands (15 m and up). " +
                        "A K-index of 5 or more means a geomagnetic storm, which often degrades HF and polar paths. " +
                        "These are general indicators, not a forecast for a specific path.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
