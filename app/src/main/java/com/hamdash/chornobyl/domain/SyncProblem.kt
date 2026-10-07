package com.hamdash.chornobyl.domain

/**
 * Why a catalog sync failed. Kept as data (not text) so the UI can show it in the user's language,
 * and so it can be stored as a short code.
 */
sealed interface SyncProblem {
    data object HttpsRequired : SyncProblem
    data object TooLarge : SyncProblem
    data object NotJson : SyncProblem
    data object NotObject : SyncProblem
    data class NewerSchema(val found: Int, val supported: Int) : SyncProblem
    data object NoValidEntries : SyncProblem
    data class HttpStatus(val code: Int) : SyncProblem
    data object Unreachable : SyncProblem

    fun encode(): String = when (this) {
        HttpsRequired -> "https"
        TooLarge -> "too_large"
        NotJson -> "not_json"
        NotObject -> "not_object"
        is NewerSchema -> "schema:$found:$supported"
        NoValidEntries -> "no_entries"
        is HttpStatus -> "http:$code"
        Unreachable -> "unreachable"
    }

    companion object {
        fun decode(code: String?): SyncProblem? {
            if (code == null) return null
            val parts = code.split(':')
            return when (parts[0]) {
                "https" -> HttpsRequired
                "too_large" -> TooLarge
                "not_json" -> NotJson
                "not_object" -> NotObject
                "schema" -> NewerSchema(parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2)?.toIntOrNull() ?: 0)
                "no_entries" -> NoValidEntries
                "http" -> HttpStatus(parts.getOrNull(1)?.toIntOrNull() ?: 0)
                "unreachable" -> Unreachable
                else -> Unreachable
            }
        }
    }
}
