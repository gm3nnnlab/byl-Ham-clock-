package com.hamdash.chornobyl.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.domain.haversineKm
import com.hamdash.chornobyl.location.LocationState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterDetailSheet(
    repeater: Repeater,
    location: LocationState,
    unit: DistanceUnit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val missing = noData()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(repeater.callsign, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(repeater.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ModeChip(repeater.mode)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(if (isFavorite) R.string.fav_remove else R.string.fav_add),
                        tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DetailSection(stringResource(R.string.sec_frequency))
            LabeledRow(stringResource(R.string.lbl_rx), mhzText(formatMHz(repeater.rxMHz)), mono = true)
            LabeledRow(stringResource(R.string.lbl_tx), mhzText(formatMHz(repeater.txMHz)), mono = true)
            LabeledRow(stringResource(R.string.lbl_offset), offsetText(repeater.offsetMHz), mono = true)
            repeater.bandName?.let { LabeledRow(stringResource(R.string.lbl_band), it) }

            DetailSection(stringResource(R.string.sec_parameters))
            LabeledRow(stringResource(R.string.lbl_mode), repeater.mode.displayName())
            repeater.ctcssHz?.let {
                LabeledRow(stringResource(R.string.lbl_ctcss), stringResource(R.string.value_hz, trimTone(it)), mono = true)
            }
            if (repeater.mode == RadioMode.DMR) {
                LabeledRow(
                    stringResource(R.string.lbl_color_code),
                    repeater.colorCode?.let { stringResource(R.string.value_cc, it) } ?: missing,
                )
                LabeledRow(
                    stringResource(R.string.lbl_timeslot),
                    repeater.timeslot?.let { stringResource(R.string.value_ts, it) } ?: missing,
                )
                LabeledRow(
                    stringResource(R.string.lbl_talkgroups),
                    repeater.talkgroupIds.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { stringResource(R.string.value_tg, it) } ?: missing,
                )
            }

            DetailSection(stringResource(R.string.sec_location))
            LabeledRow(
                stringResource(R.string.lbl_coordinates),
                String.format(Locale.US, "%.4f, %.4f", repeater.latitude, repeater.longitude),
                mono = true,
            )
            LabeledRow(
                stringResource(R.string.lbl_elevation),
                repeater.elevationM?.let { stringResource(R.string.value_m, it) } ?: missing,
            )
            (location as? LocationState.Fix)?.let { fix ->
                LabeledRow(
                    stringResource(R.string.lbl_distance),
                    distanceText(haversineKm(fix.latitude, fix.longitude, repeater.latitude, repeater.longitude), unit),
                )
            }

            DetailSection(stringResource(R.string.sec_source))
            LabeledRow(stringResource(R.string.lbl_source), repeater.source)
            LabeledRow(stringResource(R.string.lbl_last_updated), repeater.lastUpdated.ifEmpty { missing })
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String) {
    Spacer(Modifier.height(10.dp))
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

private fun trimTone(hz: Double): String = if (hz % 1.0 == 0.0) hz.toInt().toString() else String.format(Locale.US, "%.1f", hz)
