package org.akinosoft.akinoclock.calendar.model

import java.time.LocalDate
import java.time.YearMonth

data class DayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasEvents: Boolean,
)

data class MonthGrid(val month: YearMonth, val cells: List<DayCell>)
