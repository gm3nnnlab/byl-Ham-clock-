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
import androidx.compose.material.icons.filled.Equalizer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.ui.common.ScreenScaffold

private data class MoreItem(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreScreen(
    onOpenBands: () -> Unit,
    onOpenDigital: () -> Unit,
    onOpenPropagation: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val items = listOf(
        MoreItem("Bands", "Amateur band reference: ranges, modes, purpose", Icons.Filled.Equalizer, onOpenBands),
        MoreItem("Digital", "DMR, D-STAR, C4FM, APRS, FT8, JS8Call and more", Icons.Filled.Memory, onOpenDigital),
        MoreItem("Propagation", "Solar flux, K-index, A-index, flares", Icons.Filled.WbSunny, onOpenPropagation),
        MoreItem("Settings", "Time zone, theme, sync, privacy", Icons.Filled.Settings, onOpenSettings),
        MoreItem("About", "Data sources, limits and safety notes", Icons.Filled.Info, onOpenAbout),
    )
    ScreenScaffold(title = "More") { padding ->
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
