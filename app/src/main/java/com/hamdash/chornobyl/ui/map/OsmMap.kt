package com.hamdash.chornobyl.ui.map

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.ui.theme.ModeColors
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

private const val INITIAL_LAT = 50.8
private const val INITIAL_LON = 30.5
private const val INITIAL_ZOOM = 8.0
private const val MY_LOCATION_ZOOM = 13.0

/** OpenStreetMap map (osmdroid). Tiles are cached on disk, so areas you have viewed stay available offline. */
@Composable
fun OsmMap(
    repeaters: List<Repeater>,
    userLocation: LocationState.Fix?,
    modifier: Modifier = Modifier,
    recenterRequest: Int = 0,
    tileUrl: String = "",
    onRepeaterClick: (Repeater) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val myLocationTitle = stringResource(R.string.map_my_location)
    val onClick by rememberUpdatedState(onRepeaterClick)
    val latestUser by rememberUpdatedState(userLocation)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 3.0
            maxZoomLevel = 18.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(INITIAL_ZOOM)
            controller.setCenter(GeoPoint(INITIAL_LAT, INITIAL_LON))
            // Keep the parent (e.g. the dashboard's scrolling column) from stealing pan gestures.
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(tileUrl) {
        mapView.setTileSource(if (tileUrl.isBlank()) TileSourceFactory.MAPNIK else TemplateTileSource(tileUrl))
        mapView.invalidate()
    }

    LaunchedEffect(dark) {
        mapView.overlayManager.tilesOverlay.setColorFilter(if (dark) TilesOverlay.INVERT_COLORS else null)
        mapView.invalidate()
    }

    LaunchedEffect(repeaters, userLocation, myLocationTitle) {
        mapView.overlays.removeAll { it is Marker }
        repeaters.forEach { repeater ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(repeater.latitude, repeater.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = circleIcon(context, ModeColors.of(repeater.mode).toArgb(), 22)
                title = repeater.callsign
                setOnMarkerClickListener { _, _ ->
                    onClick(repeater)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        userLocation?.let { fix ->
            val me = Marker(mapView).apply {
                position = GeoPoint(fix.latitude, fix.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = circleIcon(context, AndroidColor.parseColor("#2979FF"), 18)
                title = myLocationTitle
                setOnMarkerClickListener { _, _ -> true }
            }
            mapView.overlays.add(me)
        }
        mapView.invalidate()
    }

    LaunchedEffect(recenterRequest) {
        if (recenterRequest > 0) {
            latestUser?.let { fix ->
                mapView.controller.setZoom(MY_LOCATION_ZOOM)
                mapView.controller.animateTo(GeoPoint(fix.latitude, fix.longitude))
            }
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            Text(
                stringResource(if (tileUrl.isBlank()) R.string.map_attribution_osm else R.string.map_attribution_custom),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Tile source for a user supplied https template such as https://host/{z}/{x}/{y}.png. */
private class TemplateTileSource(private val template: String) :
    OnlineTileSourceBase("Custom-${template.hashCode()}", 0, 19, 256, "", arrayOf(template)) {
    override fun getTileURLString(pMapTileIndex: Long): String = template
        .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
        .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
        .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())
}

private fun circleIcon(context: Context, color: Int, sizeDp: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke((2 * density).toInt(), AndroidColor.WHITE)
        setSize(px, px)
    }
}
