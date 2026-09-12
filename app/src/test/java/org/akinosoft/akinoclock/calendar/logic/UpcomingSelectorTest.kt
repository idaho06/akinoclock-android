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
    fun `result is capped at 3`() {
        // now is Sep 25; Sep has 30 days, so all 5 of these stay within the current month.
        val instances = (0 until 5).map { timedEvent(it.toLong(), now.toLocalDate().plusDays(it.toLong()), 10) }

        val result = UpcomingSelector.select(instances, now)

        assertEquals(3, result.size)
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

    @Test
    fun `events in the next month are excluded even within the top N by date`() {
        // now is Sep 25; Sep 30 is the last day of the current month, Oct 1 is next month.
        val lastDayOfMonth = timedEvent(11, LocalDate.of(2026, 9, 30), 10)
        val nextMonth = timedEvent(12, LocalDate.of(2026, 10, 1), 10)

        val result = UpcomingSelector.select(listOf(lastDayOfMonth, nextMonth), now)

        assertEquals(listOf(lastDayOfMonth), result)
    }

    @Test
    fun `near month-end fewer than 3 qualifying events are returned without padding`() {
        val nowNearMonthEnd = ZonedDateTime.of(2026, 9, 29, 12, 0, 0, 0, madrid)
        val lastDayOfMonth = timedEvent(13, LocalDate.of(2026, 9, 30), 10)
        val nextMonth = timedEvent(14, LocalDate.of(2026, 10, 1), 10)

        val result = UpcomingSelector.select(listOf(lastDayOfMonth, nextMonth), nowNearMonthEnd)

        assertEquals(listOf(lastDayOfMonth), result)
    }
}
