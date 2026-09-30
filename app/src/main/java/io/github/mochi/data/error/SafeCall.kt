package io.github.mochi.data.error

import kotlinx.coroutines.CancellationException

/** Runs [block], mapping any failure to a [MochiError] so callers only ever see clean, user-facing messages. */
suspend fun <T> safeCall(block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    throw e.toMochiError()
}
