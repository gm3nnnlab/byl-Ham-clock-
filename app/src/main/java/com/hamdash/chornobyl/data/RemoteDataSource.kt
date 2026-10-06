package com.hamdash.chornobyl.data

import com.hamdash.chornobyl.domain.RadioCatalog
import com.hamdash.chornobyl.network.RawApi
import com.hamdash.chornobyl.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

sealed interface RemoteResult {
    data object NotConfigured : RemoteResult
    data class Failure(val message: String) : RemoteResult
    data class Success(val catalog: RadioCatalog, val rejected: List<String>) : RemoteResult
}

/** Anything that can supply a catalog. Implement this to plug in a real service later. */
interface RemoteDataSource {
    suspend fun fetchCatalog(): RemoteResult
}

/** Downloads a catalog JSON from the https URL the user configured in Settings. */
class HttpCatalogRemoteDataSource(
    private val api: RawApi,
    private val settings: SettingsRepository,
) : RemoteDataSource {

    override suspend fun fetchCatalog(): RemoteResult {
        val url = settings.current().catalogUrl.trim()
        if (url.isEmpty()) return RemoteResult.NotConfigured
        if (!url.startsWith("https://")) {
            return RemoteResult.Failure("The catalog URL must start with https://. Edit it in Settings > Data source.")
        }
        return try {
            val text = withContext(Dispatchers.IO) {
                val body = api.fetch(url)
                if (body.contentLength() > MAX_BYTES) throw CatalogFormatException("The catalog is larger than 5 MB.")
                body.string()
            }
            val parsed = CatalogParser.parse(text)
            if (parsed.catalog.isEmpty) {
                RemoteResult.Failure("The server returned no valid entries, so the local data was kept.")
            } else {
                RemoteResult.Success(parsed.catalog, parsed.rejected)
            }
        } catch (e: CatalogFormatException) {
            RemoteResult.Failure("${e.message} The local data was kept.")
        } catch (e: HttpException) {
            RemoteResult.Failure("The server answered HTTP ${e.code()}. The local data was kept; try again later.")
        } catch (e: IOException) {
            RemoteResult.Failure("The server could not be reached. The local data was kept; check the connection or the URL.")
        }
    }

    private companion object {
        const val MAX_BYTES = 5L * 1024 * 1024
    }
}
