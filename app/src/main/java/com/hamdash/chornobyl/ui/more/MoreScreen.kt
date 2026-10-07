package com.hamdash.chornobyl.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.ui.common.ScreenScaffold

private data class MoreItem(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreScreen(
    onOpenBands: () -> Unit,
    onOpenDigital: () -> Unit,
    onOpenPropagation: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenFpv: () -> Unit,
    onOpenZone: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val items = listOf(
        MoreItem(stringResource(R.string.more_weather_title), stringResource(R.string.more_weather_sub), Icons.Filled.Cloud, onOpenWeather),
        MoreItem(stringResource(R.string.more_fpv_title), stringResource(R.string.more_fpv_sub), Icons.Filled.FlightTakeoff, onOpenFpv),
        MoreItem(stringResource(R.string.more_zone_title), stringResource(R.string.more_zone_sub), Icons.Filled.HistoryEdu, onOpenZone),
        MoreItem(stringResource(R.string.more_bands_title), stringResource(R.string.more_bands_sub), Icons.Filled.Equalizer, onOpenBands),
        MoreItem(stringResource(R.string.more_digital_title), stringResource(R.string.more_digital_sub), Icons.Filled.Memory, onOpenDigital),
        MoreItem(stringResource(R.string.more_prop_title), stringResource(R.string.more_prop_sub), Icons.Filled.WbSunny, onOpenPropagation),
        MoreItem(stringResource(R.string.more_settings_title), stringResource(R.string.more_settings_sub), Icons.Filled.Settings, onOpenSettings),
        MoreItem(stringResource(R.string.more_about_title), stringResource(R.string.more_about_sub), Icons.Filled.Info, onOpenAbout),
    )
    ScreenScaffold(title = stringResource(R.string.more_title)) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            items.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index < items.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
