package org.akinosoft.akinoclock.calendar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * Refreshing (a query plus the [CalendarRepository.changes] subscription) only runs
 * between [start] and [stop] — mirrors `ClockView.start()/stop()`, driven by the host
 * Activity's `onStart`/`onStop` so the ContentObserver isn't held while not visible.
 * Calling [start] while already started is a no-op, including the load it would
 * otherwise trigger — call [refresh] directly to force a reload while started (e.g.
 * after a permission-dialog result).
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

    fun start() {
        if (changesJob?.isActive == true) return
        changesJob = viewModelScope.launch {
            repository.changes().collect { refresh() }
        }
        refresh()
    }

    fun stop() {
        changesJob?.cancel()
        changesJob = null
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (!permissionChecker.hasReadCalendar()) {
            _uiState.value = CalendarUiState.NotGranted
            return
        }

        val now = ZonedDateTime.now(clock)
        val month = YearMonth.from(now)
        val window = QueryWindow.forGrid(month, clock.zone)

        try {
            val instances = withContext(ioDispatcher) {
                repository.instancesBetween(window.beginMillis, window.endMillis)
            }

            // A SecurityException from the resolver surfaces as an empty list rather than
            // an exception, so re-check permission here instead of trusting an empty result.
            if (!permissionChecker.hasReadCalendar()) {
                _uiState.value = CalendarUiState.NotGranted
                return
            }

            val eventDays = InstanceMapper.eventDays(instances, clock.zone)
            val grid = MonthGridBuilder.build(month, now.toLocalDate(), eventDays)
            val todayList = UpcomingSelector.select(instances, now)
            _uiState.value = CalendarUiState.Granted(grid, todayList)
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
