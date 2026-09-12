package org.akinosoft.akinoclock.util

import java.time.Duration

/** Shared backoff arithmetic for a periodic-refresh loop (RSS, weather): the normal interval while
 * healthy, stepping through [BACKOFF_MINUTES] on consecutive failures and resetting on success. */
class RefreshBackoff(private val normalInterval: Duration) {

    private var failureIndex = -1

    fun nextDelay(): Duration =
        if (failureIndex >= 0) Duration.ofMinutes(BACKOFF_MINUTES[failureIndex]) else normalInterval

    fun onFailure() {
        failureIndex = (failureIndex + 1).coerceAtMost(BACKOFF_MINUTES.lastIndex)
    }

    fun onSuccess() {
        failureIndex = -1
    }

    fun reset() {
        failureIndex = -1
    }

    fun isBackingOff(): Boolean = failureIndex >= 0

    private companion object {
        val BACKOFF_MINUTES = listOf(1L, 2L, 4L, 8L, 15L)
    }
}
