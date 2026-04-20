package com.omerkaya.sperrmuellfinder.core.util

/**
 * A generic class that holds a value with its loading status.
 * Rules.md compliant - Core utility class
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}