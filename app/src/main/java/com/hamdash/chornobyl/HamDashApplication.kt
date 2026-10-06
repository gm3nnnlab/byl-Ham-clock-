package com.hamdash.chornobyl

import android.app.Application
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class HamDashApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
        container = AppContainer(this)
        container.appScope.launch {
            runCatching { container.repository.initialize() }
            runCatching { container.propagationRepository.refreshIfStale() }
        }
    }

    private fun configureOsmdroid() {
        val base = File(cacheDir, "osmdroid")
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
        }
    }
}
