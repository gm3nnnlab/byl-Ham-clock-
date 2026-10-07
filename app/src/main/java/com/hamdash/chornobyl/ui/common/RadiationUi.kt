package com.hamdash.chornobyl.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.RadiationBand
import com.hamdash.chornobyl.domain.RadiationCell
import com.hamdash.chornobyl.domain.RadiationProblem
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.radiationBand
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

fun RadiationBand.color(): Color = when (this) {
    RadiationBand.UP_TO_0_3 -> Color(0xFF2EBD6B)
    RadiationBand.UP_TO_1 -> Color(0xFFE8C21B)
    RadiationBand.UP_TO_10 -> Color(0xFFEF8A1B)
    RadiationBand.OVER_10 -> Color(0xFFE5484D)
}

@Composable
fun RadiationBand.rangeLabel(): String = stringResource(
    when (this) {
        RadiationBand.UP_TO_0_3 -> R.string.rad_band_1
        RadiationBand.UP_TO_1 -> R.string.rad_band_2
        RadiationBand.UP_TO_10 -> R.string.rad_band_3
        RadiationBand.OVER_10 -> R.string.rad_band_4
    },
)

@Composable
fun usvText(value: Double): String {
    val text = when {
        value >= 100 -> String.format(Locale.US, "%.0f", value)
        value >= 10 -> String.format(Locale.US, "%.1f", value)
        else -> String.format(Locale.US, "%.2f", value)
    }
    return stringResource(R.string.value_usvh, text)
}

@Composable
fun RadiationProblem.message(received: Int, skipped: Int): String = when (this) {
    RadiationProblem.OFFLINE -> stringResource(R.string.rad_err_offline)
    RadiationProblem.METERED_BLOCKED -> stringResource(R.string.rad_err_metered)
    RadiationProblem.UNREACHABLE -> stringResource(R.string.rad_err_unreachable)
    RadiationProblem.UNREADABLE -> stringResource(R.string.rad_err_unreadable)
    RadiationProblem.EMPTY -> stringResource(R.string.rad_err_empty)
    RadiationProblem.NO_USABLE -> stringResource(R.string.rad_err_no_usable, received, skipped)
}

@Composable
fun RadiationLegend(modifier: Modifier = Modifier) {
    Column(modifier) {
        RadiationBand.entries.forEach { band ->
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(14.dp).clip(CircleShape).background(band.color()))
                Spacer(Modifier.width(8.dp))
                Text(band.rangeLabel(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Small legend drawn over the map while the radiation layer is on. */
@Composable
fun RadiationLegendCard(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape = MaterialTheme.shapes.small) {
        RadiationLegend(Modifier.padding(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadiationDetailSheet(cell: RadiationCell, zone: ZoneId, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text(stringResource(R.string.rad_detail_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(18.dp).clip(CircleShape).background(radiationBand(cell.medianUsvH).color()))
                Spacer(Modifier.width(8.dp))
                Text(usvText(cell.medianUsvH), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.rad_detail_median_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LabeledRow(stringResource(R.string.rad_detail_max), usvText(cell.maxUsvH), Modifier.padding(top = 8.dp), mono = true)
            LabeledRow(stringResource(R.string.rad_detail_count), cell.count.toString(), mono = true)
            LabeledRow(
                stringResource(R.string.rad_detail_latest),
                stringResource(R.string.prop_utc_value, TimeFormats.dateTime(Instant.ofEpochSecond(cell.latestEpoch), ZoneId.of("UTC"))),
            )
            LabeledRow(stringResource(R.string.rad_detail_area), String.format(Locale.US, "%.3f, %.3f", cell.latitude, cell.longitude), mono = true)
            if (cell.estimated) {
                Text(
                    stringResource(R.string.rad_estimate_flag),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                stringResource(R.string.rad_detail_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(R.string.rad_warning_short),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
