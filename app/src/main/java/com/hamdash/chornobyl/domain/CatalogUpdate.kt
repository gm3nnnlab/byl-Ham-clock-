package com.hamdash.chornobyl.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Whether the catalog bundled with a newer app version should replace the stored one. Only data that came
 * from the bundle is replaced; a catalog downloaded from the user's own source is never overwritten.
 */
fun bundledCatalogIsNewer(storedOrigin: DataOrigin, storedUpdated: String?, bundledUpdated: String?): Boolean {
    if (storedOrigin != DataOrigin.BUNDLED) return false
    val bundled = parseDate(bundledUpdated) ?: return false
    val stored = parseDate(storedUpdated) ?: return true
    return bundled.isAfter(stored)
}

private fun parseDate(value: String?): LocalDate? =
    try {
        value?.let { LocalDate.parse(it.trim()) }
    } catch (e: DateTimeParseException) {
        null
    }
