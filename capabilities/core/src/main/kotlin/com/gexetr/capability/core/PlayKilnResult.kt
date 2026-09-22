package com.gexetr.capability.core

sealed interface PlayKilnResult<out T> {
    data class Success<T>(val value: T) : PlayKilnResult<T>

    data class Failure(
        val reason: Reason,
        val cause: Throwable? = null,
    ) : PlayKilnResult<Nothing>

    enum class Reason {
        NOT_AVAILABLE,
        NOT_CONFIGURED,
        NO_CONSENT,
        NETWORK,
        POLICY_BLOCKED,
        UNKNOWN,
    }
}

fun <T> PlayKilnResult<T>.getOrNull(): T? = when (this) {
    is PlayKilnResult.Success -> value
    is PlayKilnResult.Failure -> null
}

inline fun <T, R> PlayKilnResult<T>.map(transform: (T) -> R): PlayKilnResult<R> = when (this) {
    is PlayKilnResult.Success -> PlayKilnResult.Success(transform(value))
    is PlayKilnResult.Failure -> this
}
