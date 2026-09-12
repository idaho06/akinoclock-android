package org.akinosoft.akinoclock.calendar.logic

import java.time.YearMonth
import java.time.ZonedDateTime
import org.akinosoft.akinoclock.calendar.model.EventInstance

private const val DEFAULT_MAX = 3

object UpcomingSelector {

    fun select(instances: List<EventInstance>, now: ZonedDateTime, max: Int = DEFAULT_MAX): List<EventInstance> {
        val today = now.toLocalDate()
        val month = YearMonth.from(today)
        return instances
            .filter { !it.start.toLocalDate().isBefore(today) }
            .filter { YearMonth.from(it.start.toLocalDate()) == month }
            .sortedWith(compareBy({ it.start.toLocalDate() }, { !it.allDay }, { it.start }))
            .take(max)
    }
}
