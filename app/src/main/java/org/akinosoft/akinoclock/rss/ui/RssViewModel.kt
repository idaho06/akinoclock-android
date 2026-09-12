package org.akinosoft.akinoclock.rss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.rss.model.FeedStatus
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.rss.model.RssUiState
import org.akinosoft.akinoclock.util.RefreshBackoff

/**
 * Drives [RssRepository.refresh] on a schedule while [start]ed: immediately if the feed list
 * changed or the last success is stale enough (avoids hammering on every rotation/recreation),
 * then every [NORMAL_INTERVAL] while running, with backoff on failure that resets on the next
 * success. [stop] cancels everything, mirroring `ClockView`/`CalendarViewModel`'s start()/stop().
 *
 * [feedsFlow] (backed by `SettingsRepository.feeds`) can change at any time, including while
 * [stop]ped — [schedulingLoop] reacts to every emission via [collectLatest], which both restarts
 * scheduling immediately for a changed list (cancelling any pending `delay` for the old one) and,
 * by comparing against [lastRefreshedFeeds], catches a change that happened entirely while
 * stopped (no separate watcher needed: the next [start] just re-subscribes and sees a list that
 * doesn't match what was last actually refreshed).
 */
class RssViewModel(
    private val repository: RssRepository,
    feedsFlow: Flow<List<FeedConfig>>,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val feedsState: StateFlow<List<FeedConfig>> =
        feedsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = emptyList())

    private val _uiState = MutableStateFlow<RssUiState>(RssUiState.Empty(noFeeds = false))
    val uiState: StateFlow<RssUiState> = _uiState.asStateFlow()

    private var lastSuccess: Instant? = null
    private var lastRefreshedFeeds: List<FeedConfig>? = null
    private val backoff = RefreshBackoff(NORMAL_INTERVAL)

    private var observeJob: Job? = null
    private var schedulingJob: Job? = null

    fun start() {
        if (observeJob?.isActive != true) {
            observeJob = viewModelScope.launch {
                combine(feedsState, repository.headlines(), repository.status(), ::computeState)
                    .collect { _uiState.value = it }
            }
        }
        if (schedulingJob?.isActive != true) {
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
        viewModelScope.launch { doRefresh(feedsState.value) }
    }

    /** [collectLatest] cancels the in-flight loop (including any pending `delay`) and restarts
     * from scratch on every feed-list emission, so a live change is picked up immediately without
     * a separate watcher job. Skips scheduling entirely while there are no feeds. */
    private suspend fun schedulingLoop() {
        feedsState.collectLatest { feeds ->
            if (feeds.isEmpty()) return@collectLatest
            if (feeds != lastRefreshedFeeds) backoff.reset()
            if (isDueForImmediateRefresh(feeds)) doRefresh(feeds)
            while (true) {
                delay(nextDelayMillis())
                doRefresh(feeds)
            }
        }
    }

    private fun isDueForImmediateRefresh(feeds: List<FeedConfig>): Boolean {
        if (feeds != lastRefreshedFeeds) return true
        val since = lastSuccess ?: return true
        return olderThan(since, IMMEDIATE_REFRESH_THRESHOLD)
    }

    private fun olderThan(instant: Instant, threshold: Duration): Boolean =
        Duration.between(instant, Instant.now(clock)) >= threshold

    private fun nextDelayMillis(): Long = backoff.nextDelay().toMillis()

    private suspend fun doRefresh(feeds: List<FeedConfig>) {
        val outcome = repository.refresh(feeds)
        lastRefreshedFeeds = feeds
        if (outcome == RefreshOutcome.ALL_FAILED) {
            backoff.onFailure()
        } else {
            backoff.onSuccess()
            lastSuccess = Instant.now(clock)
        }
    }

    private fun computeState(
        feeds: List<FeedConfig>,
        headlines: List<Headline>,
        status: Map<String, FeedStatus>,
    ): RssUiState = when {
        feeds.isEmpty() -> RssUiState.Empty(noFeeds = true)
        headlines.isEmpty() -> RssUiState.Empty(noFeeds = false)
        else -> RssUiState.Showing(headlines, stale = isStale(feeds, status))
    }

    /** Stale per the freshness rule: every configured feed's latest attempt failed, or the
     * newest successful fetch is older than twice the normal refresh interval. */
    private fun isStale(feeds: List<FeedConfig>, status: Map<String, FeedStatus>): Boolean {
        val relevant = feeds.map { status[it.url] }
        val allFailedLastAttempt = relevant.isNotEmpty() && relevant.all { it?.lastError != null }
        val newestSuccess = relevant.mapNotNull { it?.lastSuccess }.maxOrNull()
        val successTooOld = newestSuccess != null && olderThan(newestSuccess, STALE_THRESHOLD)
        return allFailedLastAttempt || successTooOld
    }

    class Factory(
        private val repository: RssRepository,
        private val feedsFlow: Flow<List<FeedConfig>>,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RssViewModel(repository, feedsFlow, clock) as T
    }

    private companion object {
        val NORMAL_INTERVAL: Duration = Duration.ofMinutes(30)
        val IMMEDIATE_REFRESH_THRESHOLD: Duration = Duration.ofMinutes(5)
        val STALE_THRESHOLD: Duration = NORMAL_INTERVAL.multipliedBy(2)
    }
}
