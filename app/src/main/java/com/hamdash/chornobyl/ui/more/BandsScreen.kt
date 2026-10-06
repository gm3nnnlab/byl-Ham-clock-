package com.hamdash.chornobyl.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.domain.Band
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.ErrorView
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.UiState
import com.hamdash.chornobyl.ui.theme.NumberFont

@Composable
fun BandsScreen(onBack: () -> Unit, viewModel: ReferenceViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val bands by viewModel.bands.collectAsStateWithLifecycle()
    ScreenScaffold(title = "Bands", onBack = onBack) { padding ->
        val modifier = Modifier.padding(padding)
        when (val state = bands) {
            UiState.Loading -> LoadingView(modifier)
            UiState.Empty -> EmptyView("No bands", "The band reference is empty. Clear the cache in Settings to restore the bundled reference.", modifier)
            is UiState.Error -> ErrorView(state.message, onRetry = null, modifier = modifier)
            is UiState.Content -> LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionCard("IMPORTANT") {
                        Text(
                            "Ranges are a general IARU Region 1 reference. Allocations, power limits and modes differ by country " +
                                "and by license class. Check your current national frequency allocation plan before transmitting.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(state.data, key = { it.name }) { band -> BandCard(band) }
            }
        }
    }
}

@Composable
private fun BandCard(band: Band) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
            Text(band.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                "${formatMHz(band.startMHz)} - ${formatMHz(band.endMHz)} MHz",
                fontFamily = NumberFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LabeledRow("Typical modes", band.typicalModes)
            LabeledRow("Purpose", band.purpose)
            if (band.notes.isNotEmpty()) LabeledRow("Notes", band.notes)
        }
    }
}
