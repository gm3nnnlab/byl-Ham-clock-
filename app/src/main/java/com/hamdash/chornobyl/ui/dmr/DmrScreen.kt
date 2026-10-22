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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.Talkgroup
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.ErrorView
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.asString
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.mhzText
import com.hamdash.chornobyl.ui.common.noData
import com.hamdash.chornobyl.ui.common.talkgroupsText
import com.hamdash.chornobyl.ui.common.offsetText
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.RepeaterStatusBadge
import com.hamdash.chornobyl.ui.theme.NumberFont

@Composable
fun DmrScreen(viewModel: DmrViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.dmr_title)) { padding ->
        val error = state.error
        val selected = state.selected
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            error != null -> ErrorView(error.asString(), onRetry = null, modifier = Modifier.padding(padding))
            selected == null -> EmptyView(
                title = stringResource(R.string.dmr_none_title),
                message = stringResource(R.string.dmr_none_msg),
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
                    Text(stringResource(R.string.dmr_talkgroups), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        singleLine = true,
                        label = { Text(stringResource(R.string.dmr_search)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.talkgroups.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (state.totalTalkgroups == 0) R.string.dmr_tg_none_published else R.string.dmr_tg_none_match),
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
            SectionCard(stringResource(R.string.dmr_sec_frequency), Modifier.weight(3f)) {
                BigValue(mhzText(formatMHz(repeater.rxMHz)))
                Text(
                    stringResource(R.string.dmr_tx_line, formatMHz(repeater.txMHz), offsetText(repeater.offsetMHz)),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = NumberFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionCard(stringResource(R.string.dmr_sec_mode), Modifier.weight(2f)) {
                BigValue(repeater.mode.displayName())
                Text(repeater.bandName.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(stringResource(R.string.dmr_sec_cc), Modifier.weight(1f)) {
                BigValue(repeater.colorCode?.let { stringResource(R.string.value_cc, it) } ?: noData())
            }
            SectionCard(stringResource(R.string.dmr_sec_slot), Modifier.weight(1f)) {
                BigValue(repeater.timeslot?.let { stringResource(R.string.value_ts, it) } ?: noData())
            }
        }
        SectionCard(stringResource(R.string.dmr_sec_repeater)) {
            RepeaterStatusBadge(repeater.status, Modifier.padding(bottom = 6.dp))
            LabeledRow(stringResource(R.string.lbl_callsign), repeater.callsign)
            LabeledRow(stringResource(R.string.lbl_name), repeater.name)
            LabeledRow(
                stringResource(R.string.lbl_talkgroups),
                talkgroupsText(repeater.talkgroupIds) ?: noData(),
            )
            LabeledRow(stringResource(R.string.lbl_source), repeater.source)
            LabeledRow(stringResource(R.string.lbl_last_updated), repeater.lastUpdated.ifEmpty { noData() })
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
                Text(stringResource(R.string.value_tg, talkgroup.id), fontFamily = NumberFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(talkgroup.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onThisRepeater) {
                    Text(stringResource(R.string.dmr_on_this), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (talkgroup.description.isNotEmpty()) {
                Text(talkgroup.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.dmr_tg_source, talkgroup.source), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
