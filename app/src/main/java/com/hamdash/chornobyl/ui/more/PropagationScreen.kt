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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.geomagneticState
import com.hamdash.chornobyl.network.PropagationSources
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.noData
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
        title = stringResource(R.string.prop_title),
        onBack = onBack,
        actions = {
            if (snapshot.loading) {
                CircularProgressIndicator(Modifier.padding(end = 16.dp).size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = viewModel::refresh) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh)) }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(stringResource(R.string.prop_sec_weather)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueTile(stringResource(R.string.prop_sfi), data?.solarFlux?.toString() ?: noData(), Modifier.weight(1f))
                    ValueTile(
                        stringResource(R.string.dash_k_index),
                        data?.kIndex?.let { String.format(Locale.US, "%.2f", it) } ?: noData(),
                        Modifier.weight(1f),
                    )
                    ValueTile(stringResource(R.string.dash_a_index), data?.aIndex?.toString() ?: noData(), Modifier.weight(1f))
                }
            }
            SectionCard(stringResource(R.string.prop_sec_conditions)) {
                LabeledRow(stringResource(R.string.prop_geomagnetic), geomagneticState(data?.kIndex)?.displayName() ?: noData())
                LabeledRow(stringResource(R.string.prop_flare), data?.flareClass ?: noData())
                data?.flareTime?.let { LabeledRow(stringResource(R.string.prop_flare_time), it) }
                data?.kIndexTime?.let { LabeledRow(stringResource(R.string.prop_k_time), stringResource(R.string.prop_utc_value, it)) }
                LabeledRow(
                    stringResource(R.string.prop_fetched),
                    data?.takeIf { it.fetchedAtMillis > 0 }
                        ?.let {
                            stringResource(
                                R.string.prop_utc_value,
                                TimeFormats.dateTime(Instant.ofEpochMilli(it.fetchedAtMillis), ZoneId.of("UTC")),
                            )
                        }
                        ?: noData(),
                )
            }
            if (snapshot.error) {
                Text(stringResource(R.string.prop_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(stringResource(R.string.prop_sec_about)) {
                Text(
                    stringResource(R.string.prop_about_text, PropagationSources.ATTRIBUTION),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
