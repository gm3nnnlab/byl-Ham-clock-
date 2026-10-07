package com.hamdash.chornobyl.ui.radiation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.network.Safecast
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.RadiationLegend
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.message
import com.hamdash.chornobyl.ui.common.noData
import com.hamdash.chornobyl.ui.common.usvText
import com.hamdash.chornobyl.ui.weather.RefreshAction
import java.time.Instant
import java.time.ZoneId

@Composable
fun RadiationScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: RadiationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = state.radiation
    val data = snapshot.data
    val zone = remember(state.settings.timeZoneId) { TimeZoneOptions.resolve(state.settings.timeZoneId) }
    val utc = ZoneId.of("UTC")

    ScreenScaffold(
        title = stringResource(R.string.rad_title),
        onBack = onBack,
        actions = { RefreshAction(snapshot.loading, viewModel::refresh) },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WarningCard()

            snapshot.problem?.let {
                Text(it.message(snapshot.received, snapshot.skipped), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            SectionCard(stringResource(R.string.rad_summary_title)) {
                if (data == null) {
                    Text(stringResource(R.string.rad_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LabeledRow(stringResource(R.string.rad_cells), data.cells.size.toString(), mono = true)
                    LabeledRow(stringResource(R.string.rad_readings), data.readingsUsed.toString(), mono = true)
                    LabeledRow(stringResource(R.string.rad_highest), data.highest?.let { usvText(it.medianUsvH) } ?: noData(), mono = true)
                    LabeledRow(
                        stringResource(R.string.rad_newest),
                        data.newestEpoch?.let {
                            stringResource(R.string.prop_utc_value, TimeFormats.dateTime(Instant.ofEpochSecond(it), utc))
                        } ?: noData(),
                    )
                    LabeledRow(
                        stringResource(R.string.prop_fetched),
                        TimeFormats.dateTime(Instant.ofEpochMilli(data.fetchedAtMillis), zone),
                    )
                    Button(onClick = {
                        viewModel.showOnMap()
                        onOpenMap()
                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(stringResource(R.string.rad_show_map))
                    }
                }
            }

            SectionCard(stringResource(R.string.rad_legend_title)) {
                RadiationLegend()
                Text(
                    stringResource(R.string.rad_legend_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            SectionCard(stringResource(R.string.rad_source_title)) {
                Text(stringResource(R.string.rad_source_text, Safecast.ATTRIBUTION), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.rad_warning_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.rad_warning_text),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
