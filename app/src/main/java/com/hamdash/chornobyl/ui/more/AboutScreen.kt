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
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.network.PropagationSources
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "About", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("CHORNOBYL HAMDASH 0.1.0") {
                Text(
                    "A dashboard for licensed radio amateurs working in the Chornobyl region: public repeater information, " +
                        "DMR parameters, band and digital mode references, UTC and local clocks, and basic propagation data. " +
                        "It is not a navigator and is not intended for any military purpose.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard("DATA SOURCES") {
                Source("Repeaters and talkgroups", "Bundled demo catalog shipped with the app. Its repeaters are fictional test entries and are labeled DEMO DATA. Replace it with a real public source before relying on it.")
                Source("Optional remote catalog", "Any https JSON catalog you enter under Settings > Data source. Entries without a stated source are rejected. The app shows each entry's source.")
                Source("Band and digital mode reference", "Bundled general reference based on the IARU Region 1 band plan overview and the projects' official sites. Verify against your national allocation plan.")
                Source("Propagation", PropagationSources.ATTRIBUTION)
                Source("Map", "OpenStreetMap contributors (tiles via osmdroid, cached on the device for offline use).")
            }
            SectionCard("PRIVACY") {
                Text(
                    "Your position is used only to show you on the map, to sort distances and to compute sun times. It is never " +
                        "uploaded or shared, and location is requested only when you tap something that needs it. The app sends no " +
                        "analytics. Network requests go only to the sources listed above.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard("SAFETY AND LEGAL") {
                Text(
                    "For civilian amateur radio use only. The app has no function to intercept or decode private or protected " +
                        "communications, search for military frequencies, locate military sites, or track people. Transmit only " +
                        "where your license and national rules allow it.",
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
