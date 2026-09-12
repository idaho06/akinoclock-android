package org.akinosoft.akinoclock.util

import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared backoff arithmetic for a periodic-refresh loop (RSS, weather): the normal interval while
 * healthy, stepping through [BACKOFF_MINUTES] on consecutive failures and resetting on success.
 * [isBackingOffFlow] lets a caller (e.g. a ViewModel's `combine()`) react to a status change
 * without mirroring [isBackingOff] into a StateFlow of its own. */
class RefreshBackoff(private val normalInterval: Duration) {

    private var failureIndex = -1

    private val _isBackingOffFlow = MutableStateFlow(false)
    val isBackingOffFlow: StateFlow<Boolean> = _isBackingOffFlow.asStateFlow()

    fun nextDelay(): Duration =
        if (failureIndex >= 0) Duration.ofMinutes(BACKOFF_MINUTES[failureIndex]) else normalInterval

    fun onFailure() {
        failureIndex = (failureIndex + 1).coerceAtMost(BACKOFF_MINUTES.lastIndex)
        _isBackingOffFlow.value = true
    }

    fun onSuccess() {
        failureIndex = -1
        _isBackingOffFlow.value = false
    }

    fun reset() {
        failureIndex = -1
        _isBackingOffFlow.value = false
    }

    fun isBackingOff(): Boolean = failureIndex >= 0

    private companion object {
        val BACKOFF_MINUTES = listOf(1L, 2L, 4L, 8L, 15L)
    }
}
