package org.akinosoft.akinoclock.calendar.model

import java.time.LocalDate

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Granted(val grid: MonthGrid, val todayList: List<EventInstance>, val today: LocalDate) :
        CalendarUiState()
    data class NotGranted(val grid: MonthGrid) : CalendarUiState()
}
