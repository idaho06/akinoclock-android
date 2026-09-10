package org.akinosoft.akinoclock.calendar.logic

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.akinosoft.akinoclock.calendar.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcomingSelectorTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    private val now = ZonedDateTime.of(2026, 9, 25, 12, 0, 0, 0, madrid)

    private fun timedEvent(id: Long, date: LocalDate, hour: Int): EventInstance {
        val start = ZonedDateTime.of(date, LocalTime.of(hour, 0), madrid)
        return EventInstance(id, "e$id", start, start.plusHours(1), allDay = false)
    }

    @Test
    fun `today's earlier and later events plus tomorrow's are included, sorted by start`() {
        val earlyToday = timedEvent(1, now.toLocalDate(), 9)
        val lateToday = timedEvent(2, now.toLocalDate(), 18)
        val tomorrow = timedEvent(3, now.toLocalDate().plusDays(1), 8)

        val result = UpcomingSelector.select(listOf(lateToday, tomorrow, earlyToday), now)

        assertEquals(listOf(earlyToday, lateToday, tomorrow), result)
    }

    @Test
    fun `all-day events sort before timed events on the same day`() {
        val allDayToday = EventInstance(
            id = 4,
            title = "All day",
            start = ZonedDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT, ZoneOffset.UTC),
            end = ZonedDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT, ZoneOffset.UTC),
            allDay = true,
        )
        val timedToday = timedEvent(5, now.toLocalDate(), 9)

        val result = UpcomingSelector.select(listOf(timedToday, allDayToday), now)

        assertEquals(listOf(allDayToday, timedToday), result)
    }

    @Test
    fun `result is capped at 6`() {
        val instances = (0 until 10).map { timedEvent(it.toLong(), now.toLocalDate().plusDays(it.toLong()), 10) }

        val result = UpcomingSelector.select(instances, now)

        assertEquals(6, result.size)
    }

    @Test
    fun `nothing today and nothing upcoming yields an empty list`() {
        val result = UpcomingSelector.select(emptyList(), now)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `past days' events are never included`() {
        val yesterday = timedEvent(9, now.toLocalDate().minusDays(1), 10)
        val today = timedEvent(10, now.toLocalDate(), 10)

        val result = UpcomingSelector.select(listOf(yesterday, today), now)

        assertEquals(listOf(today), result)
    }
}
