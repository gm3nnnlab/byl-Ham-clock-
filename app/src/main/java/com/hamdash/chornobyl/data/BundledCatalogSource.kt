package com.hamdash.chornobyl.data

import android.content.Context
import com.hamdash.chornobyl.domain.RadioCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The offline-first seed shipped inside the APK (assets/catalog.json). */
class BundledCatalogSource(private val context: Context) {
    suspend fun load(): RadioCatalog = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        CatalogParser.parse(text).catalog
    }

    private companion object {
        const val ASSET_NAME = "catalog.json"
    }
}
