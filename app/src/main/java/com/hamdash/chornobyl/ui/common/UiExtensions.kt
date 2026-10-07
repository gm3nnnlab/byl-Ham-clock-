package com.hamdash.chornobyl.ui.common

import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.SyncProblem
import com.hamdash.chornobyl.domain.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

fun <T> Flow<List<T>>.asUiState(): Flow<UiState<List<T>>> =
    map<List<T>, UiState<List<T>>> { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
        .catch { emit(UiState.Error(uiText(R.string.err_db_read))) }

fun SyncProblem.toUiText(): UiText = when (this) {
    SyncProblem.HttpsRequired -> uiText(R.string.problem_https)
    SyncProblem.TooLarge -> uiText(R.string.problem_too_large)
    SyncProblem.NotJson -> uiText(R.string.problem_not_json)
    SyncProblem.NotObject -> uiText(R.string.problem_not_object)
    is SyncProblem.NewerSchema -> uiText(R.string.problem_newer_schema, found, supported)
    SyncProblem.NoValidEntries -> uiText(R.string.problem_no_entries)
    is SyncProblem.HttpStatus -> uiText(R.string.problem_http, code)
    SyncProblem.Unreachable -> uiText(R.string.problem_unreachable)
}

fun SyncResult.toMessage(): UiText = when (this) {
    is SyncResult.Updated ->
        if (rejectedItems > 0) uiText(R.string.sync_msg_updated_skipped, rejectedItems) else uiText(R.string.sync_msg_updated)
    SyncResult.Offline -> uiText(R.string.sync_msg_offline)
    SyncResult.SkippedMobileData -> uiText(R.string.sync_msg_skipped_mobile)
    SyncResult.NotConfigured -> uiText(R.string.sync_msg_not_configured)
    SyncResult.Busy -> uiText(R.string.sync_msg_busy)
    is SyncResult.Failed -> problem.toUiText()
}
