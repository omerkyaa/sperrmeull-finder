package com.omerkaya.sperrmuellfinder.core.util

/**
 * Extension functions for Result class
 * Rules.md compliant - Core utility functions
 */

/**
 * Returns true if this Result represents a success.
 */
fun <T> Result<T>.isSuccess(): Boolean {
    return this is Result.Success
}

/**
 * Returns true if this Result represents an error.
 */
fun <T> Result<T>.isError(): Boolean {
    return this is Result.Error
}

/**
 * Returns true if this Result represents a loading state.
 */
fun <T> Result<T>.isLoading(): Boolean {
    return this is Result.Loading

}

/**
 * Returns the encapsulated value if this Result represents success or null otherwise.
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        else -> null
    }
}

/**
 * Returns the encapsulated exception if this Result represents an error or null otherwise.
 */
fun <T> Result<T>.exceptionOrNull(): Throwable? {
    return when (this) {
        is Result.Error -> exception
        else -> null
    }
}

/**
 * Returns the encapsulated value if this Result represents success or the result of onFailure if not.
 */
inline fun <T> Result<T>.getOrElse(onFailure: (exception: Throwable?) -> T): T {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> onFailure(exception)
        is Result.Loading -> onFailure(null)
    }
}

/**
 * Returns the encapsulated value if this Result represents success or defaultValue if not.
 */
fun <T> Result<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is Result.Success -> data
        else -> defaultValue
    }
}

/**
 * Maps the encapsulated value if this Result represents success.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading
    }
}

/**
 * Maps the encapsulated exception if this Result represents an error.
 */
inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> {
    return when (this) {
        is Result.Error -> Result.Error(transform(exception))
        else -> this
    }
}
