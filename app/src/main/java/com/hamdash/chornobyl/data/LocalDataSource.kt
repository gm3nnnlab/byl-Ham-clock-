package com.hamdash.chornobyl.data

import androidx.room.withTransaction
import com.hamdash.chornobyl.database.AppDatabase
import com.hamdash.chornobyl.database.FavoriteEntity
import com.hamdash.chornobyl.database.toDomain
import com.hamdash.chornobyl.database.toEntity
import com.hamdash.chornobyl.domain.Band
import com.hamdash.chornobyl.domain.DigitalMode
import com.hamdash.chornobyl.domain.RadioCatalog
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.Talkgroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface LocalDataSource {
    val repeaters: Flow<List<Repeater>>
    val talkgroups: Flow<List<Talkgroup>>
    val bands: Flow<List<Band>>
    val digitalModes: Flow<List<DigitalMode>>
    val favoriteIds: Flow<Set<String>>

    suspend fun isCatalogEmpty(): Boolean

    /** Replaces only the sections that contain data, so a partial response never wipes other sections. */
    suspend fun replaceCatalog(catalog: RadioCatalog)

    suspend fun clearCatalog()
    suspend fun setFavorite(repeaterId: String, favorite: Boolean)
}

class RoomLocalDataSource(private val db: AppDatabase) : LocalDataSource {
    private val catalog = db.catalogDao()
    private val favorites = db.favoriteDao()

    override val repeaters = catalog.observeRepeaters().map { list -> list.map { it.toDomain() } }
    override val talkgroups = catalog.observeTalkgroups().map { list -> list.map { it.toDomain() } }
    override val bands = catalog.observeBands().map { list -> list.map { it.toDomain() } }
    override val digitalModes = catalog.observeDigitalModes().map { list -> list.map { it.toDomain() } }
    override val favoriteIds = favorites.observeIds().map { it.toSet() }

    override suspend fun isCatalogEmpty(): Boolean = catalog.totalRows() == 0

    override suspend fun replaceCatalog(catalog: RadioCatalog) {
        db.withTransaction {
            val dao = db.catalogDao()
            if (catalog.repeaters.isNotEmpty()) {
                dao.deleteRepeaters()
                dao.insertRepeaters(catalog.repeaters.map { it.toEntity() })
            }
            if (catalog.talkgroups.isNotEmpty()) {
                dao.deleteTalkgroups()
                dao.insertTalkgroups(catalog.talkgroups.map { it.toEntity() })
            }
            if (catalog.bands.isNotEmpty()) {
                dao.deleteBands()
                dao.insertBands(catalog.bands.map { it.toEntity() })
            }
            if (catalog.digitalModes.isNotEmpty()) {
                dao.deleteDigitalModes()
                dao.insertDigitalModes(catalog.digitalModes.map { it.toEntity() })
            }
        }
    }

    override suspend fun clearCatalog() {
        db.withTransaction {
            val dao = db.catalogDao()
            dao.deleteRepeaters()
            dao.deleteTalkgroups()
            dao.deleteBands()
            dao.deleteDigitalModes()
        }
    }

    override suspend fun setFavorite(repeaterId: String, favorite: Boolean) {
        if (favorite) favorites.add(FavoriteEntity(repeaterId)) else favorites.remove(repeaterId)
    }
}
