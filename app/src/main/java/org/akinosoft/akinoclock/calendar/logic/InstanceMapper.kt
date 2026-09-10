package org.akinosoft.akinoclock.calendar.logic

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.akinosoft.akinoclock.calendar.model.EventInstance

/**
 * Pure day expansion. Does not filter by any window — the caller queries only the
 * instances it cares about and passes those in.
 */
object InstanceMapper {

    fun eventDays(instances: List<EventInstance>, zone: ZoneId): Set<LocalDate> {
        val days = mutableSetOf<LocalDate>()
        for (instance in instances) {
            datesOf(instance, zone).forEach(days::add)
        }
        return days
    }

    private fun datesOf(instance: EventInstance, zone: ZoneId): Sequence<LocalDate> {
        // All-day events are stored as UTC-anchored calendar dates; reprojecting them
        // into another zone would shift which day they land on, so only timed events
        // are converted to the target zone.
        fun project(dt: ZonedDateTime) = if (instance.allDay) dt else dt.withZoneSameInstant(zone)

        val startDate = project(instance.start).toLocalDate()
        val endDate = exclusiveEndDate(project(instance.end), startDate)

        return generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }
    }

    private fun exclusiveEndDate(end: ZonedDateTime, startDate: LocalDate): LocalDate {
        val endDate = end.toLocalDate()
        val endIsExclusiveMidnight = end.toLocalTime() == LocalTime.MIDNIGHT && endDate.isAfter(startDate)
        return if (endIsExclusiveMidnight) endDate.minusDays(1) else endDate
    }
}
