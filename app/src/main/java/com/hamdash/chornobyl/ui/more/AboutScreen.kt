package com.hamdash.chornobyl.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.network.OpenMeteo
import com.hamdash.chornobyl.network.PropagationSources
import com.hamdash.chornobyl.ui.zone.StalkerNote
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    var taps by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold(title = stringResource(R.string.about_title), onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(stringResource(R.string.about_card_title), onClick = { taps++ }) {
                Text(
                    stringResource(R.string.about_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (taps >= EASTER_EGG_TAPS) {
                    StalkerNote(stringResource(R.string.stalker_get_out), Modifier.padding(top = 10.dp))
                }
            }
            SectionCard(stringResource(R.string.about_sec_sources)) {
                Source(stringResource(R.string.src_repeaters_title), stringResource(R.string.src_repeaters_body))
                Source(stringResource(R.string.src_remote_title), stringResource(R.string.src_remote_body))
                Source(stringResource(R.string.src_reference_title), stringResource(R.string.src_reference_body))
                Source(stringResource(R.string.src_propagation_title), PropagationSources.ATTRIBUTION)
                Source(stringResource(R.string.src_map_title), stringResource(R.string.src_map_body))
                Source(stringResource(R.string.src_weather_title), OpenMeteo.ATTRIBUTION)
                Source(stringResource(R.string.src_zone_title), stringResource(R.string.src_zone_body))
            }
            SectionCard(stringResource(R.string.about_sec_privacy)) {
                Text(
                    stringResource(R.string.about_privacy_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard(stringResource(R.string.about_sec_safety)) {
                Text(
                    stringResource(R.string.about_safety_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun Source(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val EASTER_EGG_TAPS = 5
