package com.hamdash.chornobyl.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LocationState {
    data object NoPermission : LocationState
    data object Disabled : LocationState
    data object Searching : LocationState
    data class Fix(val latitude: Double, val longitude: Double, val accuracyM: Float?) : LocationState
}

/**
 * Keeps the device position on the device: it only feeds the map and distance labels and is never sent anywhere.
 * Uses the platform LocationManager, so no Google Play services are required.
 */
class LocationTracker(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    private var listening = false
    private var lastFix: LocationState.Fix? = null

    private val _state = MutableStateFlow<LocationState>(computeState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastFix = location.toFix()
            _state.value = computeState()
        }

        override fun onProviderEnabled(provider: String) { _state.value = computeState() }
        override fun onProviderDisabled(provider: String) { _state.value = computeState() }

        @Deprecated("Required on API < 29 where the method is abstract.")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun anyProviderEnabled(): Boolean =
        providers.any { p -> runCatching { manager.isProviderEnabled(p) }.getOrDefault(false) }

    private fun computeState(): LocationState = when {
        !hasPermission() -> LocationState.NoPermission
        !anyProviderEnabled() -> LocationState.Disabled
        else -> lastFix ?: LocationState.Searching
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) {
            stop()
            _state.value = LocationState.NoPermission
            return
        }
        if (!listening) {
            for (provider in providers) {
                if (provider !in manager.allProviders) continue
                runCatching {
                    manager.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, listener, Looper.getMainLooper())
                    manager.getLastKnownLocation(provider)?.let { candidate ->
                        val current = lastFix
                        if (current == null) lastFix = candidate.toFix()
                    }
                }
            }
            listening = true
        }
        _state.value = computeState()
    }

    fun stop() {
        if (listening) {
            runCatching { manager.removeUpdates(listener) }
            listening = false
        }
    }

    private fun Location.toFix() =
        LocationState.Fix(latitude, longitude, if (hasAccuracy()) accuracy else null)

    private companion object {
        const val UPDATE_INTERVAL_MS = 5_000L
        const val UPDATE_DISTANCE_M = 10f
    }
}
