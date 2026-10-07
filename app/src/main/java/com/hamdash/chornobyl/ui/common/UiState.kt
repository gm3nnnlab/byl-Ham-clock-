package com.hamdash.chornobyl.ui.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(val message: UiText) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}
