package org.akinosoft.akinoclock.calendar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.calendar.logic.InstanceMapper
import org.akinosoft.akinoclock.calendar.logic.MonthGridBuilder
import org.akinosoft.akinoclock.calendar.logic.QueryWindow
import org.akinosoft.akinoclock.calendar.logic.UpcomingSelector
import org.akinosoft.akinoclock.calendar.model.CalendarUiState

private const val TAG = "CalendarViewModel"

/**
 * The [CalendarRepository.changes] subscription and the local-midnight refresh loop only run
 * between [start] and [stop] — mirrors `ClockView.start()/stop()`, driven by the host Activity's
 * `onStart`/`onStop` so the ContentObserver isn't held while not visible. [start] always triggers
 * a reload, even if already started, since `onStart` can fire again (e.g. returning from system
 * Settings) without an intervening `onStop`, and the permission state may have changed meanwhile.
 */
class CalendarViewModel(
    private val repository: CalendarRepository,
    private val permissionChecker: PermissionChecker,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var changesJob: Job? = null
    private var midnightJob: Job? = null
    private var loadJob: Job? = null

    fun start() {
        if (changesJob?.isActive != true) {
            changesJob = viewModelScope.launch {
                repository.changes().collect { refresh() }
            }
        }
        // Nothing else refreshes the grid across a local-midnight rollover if the app is never
        // backgrounded and the calendar provider stays quiet, so isToday would keep pointing at
        // yesterday until some unrelated event (e.g. an onStop/onStart cycle) happened to refresh.
        if (midnightJob?.isActive != true) {
            midnightJob = viewModelScope.launch { midnightLoop() }
        }
        refresh()
    }

    fun stop() {
        changesJob?.cancel()
        changesJob = null
        midnightJob?.cancel()
        midnightJob = null
    }

    private suspend fun midnightLoop() {
        while (true) {
            delay(millisUntilNextMidnight())
            refresh()
        }
    }

    private fun millisUntilNextMidnight(): Long {
        val now = ZonedDateTime.now(clock)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(clock.zone)
        return Duration.between(now, nextMidnight).toMillis()
    }

    /** Cancels any load still in flight, so an older query can never overwrite a newer result. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val now = ZonedDateTime.now(clock)
        val month = YearMonth.from(now)
        val today = now.toLocalDate()

        // The grid still renders (without dots) while permission is missing, so build it
        // from an empty day set up front rather than only on the granted path.
        fun notGranted() = CalendarUiState.NotGranted(MonthGridBuilder.build(month, today, emptySet()))

        if (!permissionChecker.hasReadCalendar()) {
            _uiState.value = notGranted()
            return
        }

        val window = QueryWindow.forGrid(month, clock.zone)

        try {
            val instances = withContext(ioDispatcher) {
                repository.instancesBetween(window.beginMillis, window.endMillis)
            }

            // A SecurityException from the resolver surfaces as an empty list rather than
            // an exception, so re-check permission here instead of trusting an empty result.
            if (!permissionChecker.hasReadCalendar()) {
                _uiState.value = notGranted()
                return
            }

            val eventDays = InstanceMapper.eventDays(instances, clock.zone)
            val grid = MonthGridBuilder.build(month, today, eventDays)
            val todayList = UpcomingSelector.select(instances, now)
            _uiState.value = CalendarUiState.Granted(grid, todayList, today)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "failed to load calendar data, keeping previous state", e)
        }
    }

    class Factory(
        private val repository: CalendarRepository,
        private val permissionChecker: PermissionChecker,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(repository, permissionChecker, clock) as T
    }
}
