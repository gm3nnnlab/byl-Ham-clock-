package com.hamdash.chornobyl.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.DigitalMode
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.asString
import com.hamdash.chornobyl.ui.common.ErrorView
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.UiState

@Composable
fun DigitalScreen(onBack: () -> Unit, viewModel: ReferenceViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val modes by viewModel.digitalModes.collectAsStateWithLifecycle()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold(title = stringResource(R.string.digital_title), onBack = onBack) { padding ->
        val modifier = Modifier.padding(padding)
        when (val state = modes) {
            UiState.Loading -> LoadingView(modifier)
            UiState.Empty -> EmptyView(stringResource(R.string.digital_none_title), stringResource(R.string.digital_none_msg), modifier)
            is UiState.Error -> ErrorView(state.message.asString(), onRetry = null, modifier = modifier)
            is UiState.Content -> LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.data, key = { it.id }) { mode ->
                    DigitalModeCard(
                        mode = mode,
                        expanded = expandedId == mode.id,
                        onToggle = { expandedId = if (expandedId == mode.id) null else mode.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalModeCard(mode: DigitalMode, expanded: Boolean, onToggle: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(mode.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(mode.summary, style = MaterialTheme.typography.bodyMedium)
            if (expanded) {
                Section(stringResource(R.string.digital_how), mode.principle)
                Section(stringResource(R.string.digital_use), mode.uses)
                Section(stringResource(R.string.digital_need), mode.equipment)
                mode.links.forEach { link ->
                    TextButton(onClick = { runCatching { uriHandler.openUri(link.url) } }) { Text(link.title) }
                }
            } else {
                Text(stringResource(R.string.digital_tap), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    if (body.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
