package com.hamdash.chornobyl.ui.zone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.ZonePlace
import com.hamdash.chornobyl.domain.isInsideZoneRadius
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.localized

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailSheet(place: ZonePlace, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            PlaceDetails(place)
        }
    }
}

@Composable
fun PlaceDetails(place: ZonePlace) {
    Text(place.name.localized(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        place.category.displayName(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(place.text.localized(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    place.stalker?.let { note ->
        StalkerNote(note.localized(), Modifier.padding(top = 10.dp))
    }
    if (isInsideZoneRadius(place.latitude, place.longitude)) {
        Text(
            stringResource(R.string.zone_inside_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    Text(
        stringResource(
            if (place.approximate) R.string.zone_coords_approx else R.string.zone_coords,
            String.format(java.util.Locale.US, "%.4f, %.4f", place.latitude, place.longitude),
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
    Text(
        stringResource(R.string.zone_source, place.source),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
