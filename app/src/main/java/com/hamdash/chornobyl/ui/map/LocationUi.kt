package com.hamdash.chornobyl.ui.map

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.location.LocationTracker

/** Returns a function that asks for location permission only when the user taps something that needs it. */
@Composable
fun rememberLocationPermissionRequest(tracker: LocationTracker): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        tracker.start()
    }
    return remember(tracker, launcher) {
        {
            if (tracker.hasPermission()) {
                tracker.start()
            } else {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }
}

/** Explains why there is no position yet; renders nothing once a fix is available. */
@Composable
fun LocationStatusBanner(
    state: LocationState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is LocationState.Fix) return
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val message: String
            var action: String? = null
            var onAction: (() -> Unit)? = null
            when (state) {
                LocationState.NoPermission -> {
                    message = "Location is off. Allow it to see your position on the map."
                    action = "Allow"
                    onAction = onRequestPermission
                }
                LocationState.Disabled -> {
                    message = "GPS / location services are turned off on this device."
                    action = "Open settings"
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                }
                else -> message = "Searching for a GPS fix…"
            }
            Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            val actionLabel = action
            val actionHandler = onAction
            if (actionLabel != null && actionHandler != null) {
                TextButton(onClick = actionHandler) { Text(actionLabel) }
            }
        }
    }
}
