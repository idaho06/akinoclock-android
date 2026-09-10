package org.akinosoft.akinoclock.rss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.rss.model.FeedStatus
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.rss.model.RssUiState

/**
 * Drives [RssRepository.refresh] on a schedule while [start]ed: immediately if the last success
 * is stale enough (avoids hammering on every rotation/recreation), then every [NORMAL_INTERVAL]
 * while running, with backoff on failure that resets on the next success. [stop] cancels
 * everything, mirroring `ClockView`/`CalendarViewModel`'s start()/stop() convention.
 */
class RssViewModel(
    private val repository: RssRepository,
    private val feeds: List<FeedConfig>,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<RssUiState>(RssUiState.Empty(noFeeds = feeds.isEmpty()))
    val uiState: StateFlow<RssUiState> = _uiState.asStateFlow()

    private var lastSuccess: Instant? = null
    private var backoffIndex = -1

    private var observeJob: Job? = null
    private var schedulingJob: Job? = null

    fun start() {
        if (observeJob?.isActive != true) {
            observeJob = viewModelScope.launch {
                combine(repository.headlines(), repository.status(), ::computeState).collect { _uiState.value = it }
            }
        }
        if (feeds.isNotEmpty() && schedulingJob?.isActive != true) {
            schedulingJob = viewModelScope.launch { schedulingLoop() }
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
        schedulingJob?.cancel()
        schedulingJob = null
    }

    fun refreshNow() {
        viewModelScope.launch { doRefresh() }
    }

    private suspend fun schedulingLoop() {
        if (isDueForImmediateRefresh()) doRefresh()
        while (true) {
            delay(nextDelayMillis())
            doRefresh()
        }
    }

    private fun isDueForImmediateRefresh(): Boolean {
        val since = lastSuccess ?: return true
        return olderThan(since, IMMEDIATE_REFRESH_THRESHOLD)
    }

    private fun olderThan(instant: Instant, threshold: Duration): Boolean =
        Duration.between(instant, Instant.now(clock)) >= threshold

    private fun nextDelayMillis(): Long {
        val minutes = if (backoffIndex >= 0) BACKOFF_MINUTES[backoffIndex] else NORMAL_INTERVAL.toMinutes()
        return Duration.ofMinutes(minutes).toMillis()
    }

    private suspend fun doRefresh() {
        val outcome = repository.refresh(feeds)
        if (outcome == RefreshOutcome.ALL_FAILED) {
            backoffIndex = (backoffIndex + 1).coerceAtMost(BACKOFF_MINUTES.lastIndex)
        } else {
            backoffIndex = -1
            lastSuccess = Instant.now(clock)
        }
    }

    private fun computeState(headlines: List<Headline>, status: Map<String, FeedStatus>): RssUiState = when {
        feeds.isEmpty() -> RssUiState.Empty(noFeeds = true)
        headlines.isEmpty() -> RssUiState.Empty(noFeeds = false)
        else -> RssUiState.Showing(headlines, stale = isStale(status))
    }

    /** Stale per the freshness rule: every configured feed's latest attempt failed, or the
     * newest successful fetch is older than twice the normal refresh interval. */
    private fun isStale(status: Map<String, FeedStatus>): Boolean {
        val relevant = feeds.map { status[it.url] }
        val allFailedLastAttempt = relevant.isNotEmpty() && relevant.all { it?.lastError != null }
        val newestSuccess = relevant.mapNotNull { it?.lastSuccess }.maxOrNull()
        val successTooOld = newestSuccess != null && olderThan(newestSuccess, STALE_THRESHOLD)
        return allFailedLastAttempt || successTooOld
    }

    class Factory(
        private val repository: RssRepository,
        private val feeds: List<FeedConfig>,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RssViewModel(repository, feeds, clock) as T
    }

    private companion object {
        val NORMAL_INTERVAL: Duration = Duration.ofMinutes(30)
        val IMMEDIATE_REFRESH_THRESHOLD: Duration = Duration.ofMinutes(5)
        val STALE_THRESHOLD: Duration = NORMAL_INTERVAL.multipliedBy(2)
        val BACKOFF_MINUTES = listOf(1L, 2L, 4L, 8L, 15L)
    }
}
