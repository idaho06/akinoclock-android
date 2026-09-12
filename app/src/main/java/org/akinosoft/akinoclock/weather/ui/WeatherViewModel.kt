package org.akinosoft.akinoclock.weather.ui

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
import org.akinosoft.akinoclock.util.RefreshBackoff
import org.akinosoft.akinoclock.weather.data.WeatherRefreshOutcome
import org.akinosoft.akinoclock.weather.data.WeatherRepository
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.akinosoft.akinoclock.weather.model.WeatherReport
import org.akinosoft.akinoclock.weather.model.WeatherUiState

/**
 * Drives [WeatherRepository.refresh] on a schedule while [start]ed, mirroring
 * `RssViewModel`'s start()/stop()/scheduling-loop structure with weather-specific thresholds
 * (60 min normal interval, 10 min immediate-refresh threshold). No location means no scheduling
 * at all — [WeatherUiState.NoLocation] and zero repository calls.
 */
class WeatherViewModel(
    private val repository: WeatherRepository,
    locationFlow: Flow<WeatherLocation?>,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val locationState: StateFlow<WeatherLocation?> =
        locationFlow.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.NoLocation)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastSuccess: Instant? = null
    private var lastRefreshedLocation: WeatherLocation? = null
    private val backoff = RefreshBackoff(NORMAL_INTERVAL)

    private var observeJob: Job? = null
    private var schedulingJob: Job? = null

    fun start() {
        if (observeJob?.isActive != true) {
            observeJob = viewModelScope.launch {
                // A failed refresh doesn't change the repository's report() value (the old report
                // is kept as-is), so backoff.isBackingOffFlow is what actually carries "the latest
                // attempt failed" into this recompute.
                combine(locationState, repository.report(), backoff.isBackingOffFlow, ::computeState)
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
        viewModelScope.launch { locationState.value?.let { doRefresh(it) } }
    }

    private suspend fun schedulingLoop() {
        locationState.collectLatest { location ->
            if (location == null) return@collectLatest
            repository.primeFromCache(location)
            if (location != lastRefreshedLocation) backoff.reset()
            if (isDueForImmediateRefresh(location)) doRefresh(location)
            while (true) {
                delay(backoff.nextDelay().toMillis())
                doRefresh(location)
            }
        }
    }

    private fun isDueForImmediateRefresh(location: WeatherLocation): Boolean {
        if (location != lastRefreshedLocation) return true
        val since = lastSuccess ?: return true
        return olderThan(since, IMMEDIATE_REFRESH_THRESHOLD)
    }

    private fun olderThan(instant: Instant, threshold: Duration): Boolean =
        Duration.between(instant, Instant.now(clock)) >= threshold

    private suspend fun doRefresh(location: WeatherLocation) {
        val outcome = repository.refresh(location)
        lastRefreshedLocation = location
        if (outcome == WeatherRefreshOutcome.FAILED) {
            backoff.onFailure()
        } else {
            backoff.onSuccess()
            lastSuccess = Instant.now(clock)
        }
    }

    private fun computeState(location: WeatherLocation?, report: WeatherReport?, backingOff: Boolean): WeatherUiState =
        when {
            location == null -> WeatherUiState.NoLocation
            report == null -> WeatherUiState.Loading
            else -> WeatherUiState.Showing(report, stale = isStale(report, backingOff))
        }

    /** Stale once the backoff is actively retrying (the latest attempt failed) or the shown
     * report's own [WeatherReport.fetchedAt] — repository-reported truth, not ViewModel
     * bookkeeping — is older than twice the normal refresh interval. */
    private fun isStale(report: WeatherReport, backingOff: Boolean): Boolean =
        backingOff || olderThan(report.fetchedAt, STALE_THRESHOLD)

    class Factory(
        private val repository: WeatherRepository,
        private val locationFlow: Flow<WeatherLocation?>,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(repository, locationFlow, clock) as T
    }

    private companion object {
        val NORMAL_INTERVAL: Duration = Duration.ofMinutes(60)
        val IMMEDIATE_REFRESH_THRESHOLD: Duration = Duration.ofMinutes(10)
        val STALE_THRESHOLD: Duration = NORMAL_INTERVAL.multipliedBy(2)
    }
}
