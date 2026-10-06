package com.hamdash.chornobyl.ui.common

import com.hamdash.chornobyl.domain.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

fun <T> Flow<List<T>>.asUiState(): Flow<UiState<List<T>>> =
    map<List<T>, UiState<List<T>>> { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
        .catch {
            emit(
                UiState.Error(
                    "The local database could not be read. Restart the app, or clear the cache in Settings to rebuild it.",
                ),
            )
        }

fun SyncResult.toMessage(): String = when (this) {
    is SyncResult.Updated ->
        if (rejectedItems > 0) "Database updated. $rejectedItems invalid entries were skipped." else "Database updated."
    SyncResult.Offline -> "You are offline. Showing saved data."
    SyncResult.SkippedMobileData -> "Update skipped: mobile data use is turned off in Settings."
    SyncResult.NotConfigured ->
        "No data source URL is set, so the bundled catalog stays in use. Add a URL under Settings > Data source."
    SyncResult.Busy -> "An update is already running."
    is SyncResult.Failed -> message
}
