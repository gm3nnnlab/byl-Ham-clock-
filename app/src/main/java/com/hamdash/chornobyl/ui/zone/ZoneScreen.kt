package com.hamdash.chornobyl.ui.zone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.GameEntry
import com.hamdash.chornobyl.domain.GameFact
import com.hamdash.chornobyl.domain.ZoneEvent
import com.hamdash.chornobyl.domain.ZonePlace
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.localized
import com.hamdash.chornobyl.ui.theme.NumberFont
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val StalkerAmber = Color(0xFFFFB300)

private val DISASTER_DATE: LocalDate = LocalDate.of(1986, 4, 26)

fun daysSinceDisaster(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(DISASTER_DATE, today)

@Composable
fun ZoneScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: ZoneViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.zone_tab_history),
        stringResource(R.string.zone_tab_places),
        stringResource(R.string.zone_tab_stalker),
    )

    ScreenScaffold(title = stringResource(R.string.zone_title), onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                titles.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, maxLines = 1) })
                }
            }
            val data = content
            when {
                data == null -> LoadingView()
                tab == 0 && data.history.isEmpty() -> EmptyView(stringResource(R.string.no_data), stringResource(R.string.zone_none))
                tab == 0 -> HistoryList(data.history)
                tab == 1 -> PlacesList(data.places, onOpenMap)
                else -> StalkerList(data.games, data.facts)
            }
        }
    }
}

private val listPadding = PaddingValues(16.dp)

@Composable
private fun HistoryList(events: List<ZoneEvent>) {
    LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard(stringResource(R.string.zone_since_title)) {
                Text(
                    stringResource(R.string.zone_since_days, daysSinceDisaster()),
                    fontFamily = NumberFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.zone_intro), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(events, key = { it.date + it.title.en }) { event ->
            Row {
                Text(
                    event.date,
                    fontFamily = NumberFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(92.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(event.title.localized(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val text = event.text.localized()
                    if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.zone_source, event.source),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlacesList(places: List<ZonePlace>, onOpenMap: () -> Unit) {
    LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard(
                title = stringResource(R.string.zone_places_title),
                onClick = onOpenMap,
                trailing = { androidx.compose.material3.TextButton(onClick = onOpenMap) { Text(stringResource(R.string.action_open_map)) } },
            ) {
                Text(stringResource(R.string.zone_places_text), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(places, key = { it.id }) { place ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) { PlaceDetails(place) }
            }
        }
    }
}

@Composable
private fun StalkerList(games: List<GameEntry>, facts: List<GameFact>) {
    LazyColumn(contentPadding = listPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            StalkerNote(stringResource(R.string.stalker_disclaimer))
        }
        item {
            SectionCard(stringResource(R.string.stalker_games)) {
                games.forEach { game ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(
                            game.year.toString(),
                            fontFamily = NumberFont,
                            fontWeight = FontWeight.Bold,
                            color = StalkerAmber,
                            modifier = Modifier.width(52.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(game.text.localized(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        items(facts, key = { it.title.en }) { fact ->
            SectionCard(fact.title.localized()) {
                Text(fact.text.localized(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text(
                stringResource(R.string.stalker_trademark),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Amber "PDA" styled box used for every game reference, so fiction is never mixed up with facts. */
@Composable
fun StalkerNote(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StalkerAmber.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, StalkerAmber),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                stringResource(R.string.stalker_label),
                color = StalkerAmber,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
