package org.akinosoft.akinoclock.calendar.model

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Granted(val grid: MonthGrid, val todayList: List<EventInstance>) : CalendarUiState()
    data object NotGranted : CalendarUiState()
}
