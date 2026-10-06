package com.hamdash.chornobyl.ui.dmr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.Talkgroup
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.domain.formatOffset
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.ErrorView
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.NO_DATA
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.theme.NumberFont

@Composable
fun DmrScreen(viewModel: DmrViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = "DMR RADIO") { padding ->
        val error = state.error
        val selected = state.selected
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            error != null -> ErrorView(error, onRetry = null, modifier = Modifier.padding(padding))
            selected == null -> EmptyView(
                title = "No DMR repeaters",
                message = "The local database has no DMR repeaters. Open Settings to check your data source and sync.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.repeaters, key = { it.id }) { repeater ->
                            FilterChip(
                                selected = repeater.id == selected.id,
                                onClick = { viewModel.select(repeater) },
                                label = { Text(repeater.callsign) },
                            )
                        }
                    }
                }
                item { RepeaterSummary(selected) }
                item {
                    Text("TALKGROUPS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        singleLine = true,
                        label = { Text("Search by ID, name or description") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.talkgroups.isEmpty()) {
                    item {
                        Text(
                            if (state.totalTalkgroups == 0) "No talkgroups are published in the local database." else "No talkgroups match your search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.talkgroups, key = { it.id }) { talkgroup ->
                        TalkgroupRow(talkgroup, onThisRepeater = talkgroup.id in selected.talkgroupIds)
                    }
                }
            }
        }
    }
}

@Composable
private fun BigValue(text: String) {
    Text(text, fontFamily = NumberFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1)
}

@Composable
private fun RepeaterSummary(repeater: Repeater) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("FREQUENCY", Modifier.weight(3f)) {
                BigValue("${formatMHz(repeater.rxMHz)} MHz")
                Text(
                    "TX ${formatMHz(repeater.txMHz)}  ${formatOffset(repeater.offsetMHz)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = NumberFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionCard("MODE", Modifier.weight(2f)) {
                BigValue(repeater.mode.label)
                Text(repeater.bandName ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("COLOR CODE", Modifier.weight(1f)) {
                BigValue(repeater.colorCode?.let { "CC $it" } ?: NO_DATA)
            }
            SectionCard("SLOT", Modifier.weight(1f)) {
                BigValue(repeater.timeslot?.let { "TS $it" } ?: NO_DATA)
            }
        }
        SectionCard("REPEATER") {
            LabeledRow("Callsign", repeater.callsign)
            LabeledRow("Name", repeater.name)
            LabeledRow("Talkgroups", repeater.talkgroupIds.takeIf { it.isNotEmpty() }?.joinToString(", ") { "TG $it" } ?: NO_DATA)
            LabeledRow("Source", repeater.source)
            LabeledRow("Last updated", repeater.lastUpdated.ifEmpty { NO_DATA })
        }
    }
}

@Composable
private fun TalkgroupRow(talkgroup: Talkgroup, onThisRepeater: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text("TG ${talkgroup.id}", fontFamily = NumberFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(talkgroup.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onThisRepeater) {
                    Text("on this repeater", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (talkgroup.description.isNotEmpty()) {
                Text(talkgroup.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text("Source: ${talkgroup.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
