package org.akinosoft.akinoclock.calendar.logic

import java.time.LocalDate
import java.time.YearMonth
import org.akinosoft.akinoclock.calendar.model.DayCell
import org.akinosoft.akinoclock.calendar.model.MonthGrid

object MonthGridBuilder {

    fun build(month: YearMonth, today: LocalDate, eventDays: Set<LocalDate>): MonthGrid {
        val gridStart = gridStart(month)

        val cells = (0 until GRID_SIZE).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            val inCurrentMonth = YearMonth.from(date) == month
            DayCell(
                date = date,
                inCurrentMonth = inCurrentMonth,
                isToday = date == today,
                hasEvents = inCurrentMonth && date in eventDays,
            )
        }

        return MonthGrid(month, cells)
    }
}
