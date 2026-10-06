package com.hamdash.chornobyl

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.settings.ThemeMode
import com.hamdash.chornobyl.ui.HamDashRoot
import com.hamdash.chornobyl.ui.theme.HamDashTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as HamDashApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trackLocationWhileVisible()

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = settings.themeMode == ThemeMode.DARK

            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            HamDashTheme(darkTheme = dark) {
                HamDashRoot(container)
            }
        }
    }

    /** GPS runs only while the app is on screen and only when the user keeps "Show my location" enabled. */
    private fun trackLocationWhileVisible() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    container.settingsRepository.settings
                        .map { it.showGps }
                        .distinctUntilChanged()
                        .collect { enabled ->
                            if (enabled) container.locationTracker.start() else container.locationTracker.stop()
                        }
                } finally {
                    container.locationTracker.stop()
                }
            }
        }
    }
}
