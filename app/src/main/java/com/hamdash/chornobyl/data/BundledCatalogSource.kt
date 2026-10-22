package com.hamdash.chornobyl.data

import android.content.Context
import com.hamdash.chornobyl.domain.RadioCatalog
import com.hamdash.chornobyl.domain.catalogFingerprint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BundledCatalog(val catalog: RadioCatalog, val fingerprint: String)

/** The offline-first seed shipped inside the APK (assets/catalog.json). */
class BundledCatalogSource(private val context: Context) {
    suspend fun load(): BundledCatalog = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        BundledCatalog(CatalogParser.parse(text).catalog, catalogFingerprint(text))
    }

    private companion object {
        const val ASSET_NAME = "catalog.json"
    }
}
