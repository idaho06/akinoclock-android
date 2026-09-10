package org.akinosoft.akinoclock.calendar.logic

import java.time.YearMonth
import java.time.ZoneId

data class QueryWindow(val beginMillis: Long, val endMillis: Long) {

    companion object {
        fun forGrid(month: YearMonth, zone: ZoneId): QueryWindow {
            val gridStart = gridStart(month)
            val gridEndExclusive = gridStart.plusDays(GRID_SIZE.toLong())

            val begin = gridStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = gridEndExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
            return QueryWindow(begin, end)
        }
    }
}
