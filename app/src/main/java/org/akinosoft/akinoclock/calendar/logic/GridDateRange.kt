package org.akinosoft.akinoclock.calendar.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

internal const val GRID_SIZE = 42

/** The Monday-aligned first cell of the 6x7 month grid for [month]. */
internal fun gridStart(month: YearMonth): LocalDate {
    val firstOfMonth = month.atDay(1)
    val leadingDays = ((firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7) % 7
    return firstOfMonth.minusDays(leadingDays.toLong())
}
