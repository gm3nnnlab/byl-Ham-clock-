package com.hamdash.chornobyl

import android.content.Context
import com.hamdash.chornobyl.data.BundledCatalogSource
import com.hamdash.chornobyl.data.HttpCatalogRemoteDataSource
import com.hamdash.chornobyl.data.PropagationRepository
import com.hamdash.chornobyl.data.RadioDataRepositoryImpl
import com.hamdash.chornobyl.data.RoomLocalDataSource
import com.hamdash.chornobyl.data.SyncMetaStore
import com.hamdash.chornobyl.database.AppDatabase
import com.hamdash.chornobyl.domain.AppStatus
import com.hamdash.chornobyl.domain.RadioDataRepository
import com.hamdash.chornobyl.location.LocationTracker
import com.hamdash.chornobyl.network.ConnectivityMonitor
import com.hamdash.chornobyl.network.NetworkFactory
import com.hamdash.chornobyl.settings.SettingsRepository
import com.hamdash.chornobyl.settings.appDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration

/** Manual dependency container: small app, no DI framework needed. */
class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(context.appDataStore)
    val connectivity = ConnectivityMonitor(context)
    val locationTracker = LocationTracker(context)

    private val database = AppDatabase.create(context)
    private val okHttp = NetworkFactory.okHttpClient()
    private val rawApi = NetworkFactory.rawApi(okHttp)

    val repository: RadioDataRepository = RadioDataRepositoryImpl(
        local = RoomLocalDataSource(database),
        remote = HttpCatalogRemoteDataSource(rawApi, settingsRepository),
        bundled = BundledCatalogSource(context),
        meta = SyncMetaStore(context.appDataStore),
        connectivity = connectivity,
        settings = settingsRepository,
    )

    val propagationRepository = PropagationRepository(rawApi, context.appDataStore, appScope)

    val appStatus: Flow<AppStatus> = combine(connectivity.isOnline, repository.syncState) { online, sync ->
        AppStatus(online, sync)
    }

    suspend fun clearCaches() {
        repository.clearCache()
        propagationRepository.clearCache()
        withContext(Dispatchers.IO) {
            runCatching { Configuration.getInstance().osmdroidTileCache.deleteRecursively() }
        }
    }
}
