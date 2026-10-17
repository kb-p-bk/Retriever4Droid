package com.ragdroid.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Wraps a Flow<T> emission into Flow<Result<T>>, emitting Loading first,
 * then Success for each item, and Error if the flow throws.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> =
    map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it.message ?: "Unknown error", it)) }

/**
 * Runs [block] and wraps the result in [Result.Success] or [Result.Error].
 */
suspend fun <T> runCatchingResult(block: suspend () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error", e)
    }
