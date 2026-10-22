package com.hamdash.chornobyl.domain

import java.security.MessageDigest

/** SHA-256 of the catalog text, used to notice that an app update ships a different bundled catalog. */
fun catalogFingerprint(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

/**
 * Whether the catalog bundled with this app version should replace the stored one. Only data that itself
 * came from the bundle is replaced; a catalog downloaded from the user's own source is never overwritten.
 * The content is compared rather than the "updated" date, so two bundles from the same day still differ.
 */
fun bundledCatalogChanged(storedOrigin: DataOrigin, storedFingerprint: String?, bundledFingerprint: String): Boolean =
    storedOrigin == DataOrigin.BUNDLED && storedFingerprint != bundledFingerprint
