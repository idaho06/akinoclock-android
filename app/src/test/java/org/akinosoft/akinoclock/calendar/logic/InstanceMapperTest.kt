package org.akinosoft.akinoclock.calendar.logic

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.akinosoft.akinoclock.calendar.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceMapperTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    @Test
    fun `a timed event maps to the single day it falls within`() {
        val instance = EventInstance(
            id = 1,
            title = "Standup",
            start = ZonedDateTime.of(2026, 9, 25, 18, 0, 0, 0, madrid),
            end = ZonedDateTime.of(2026, 9, 25, 19, 0, 0, 0, madrid),
            allDay = false,
        )

        val days = InstanceMapper.eventDays(listOf(instance), madrid)

        assertEquals(setOf(LocalDate.of(2026, 9, 25)), days)
    }

    @Test
    fun `an all-day event stored as UTC midnight maps to the one day it represents, exclusive end`() {
        val instance = EventInstance(
            id = 2,
            title = "Cumpleaños Ester",
            start = ZonedDateTime.of(2026, 9, 26, 0, 0, 0, 0, ZoneOffset.UTC),
            end = ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, ZoneOffset.UTC),
            allDay = true,
        )

        val days = InstanceMapper.eventDays(listOf(instance), madrid)

        assertEquals(setOf(LocalDate.of(2026, 9, 26)), days)
    }

    @Test
    fun `a timed event spanning midnight maps to both days`() {
        val instance = EventInstance(
            id = 3,
            title = "Overnight",
            start = ZonedDateTime.of(2026, 9, 25, 23, 30, 0, 0, madrid),
            end = ZonedDateTime.of(2026, 9, 26, 0, 30, 0, 0, madrid),
            allDay = false,
        )

        val days = InstanceMapper.eventDays(listOf(instance), madrid)

        assertEquals(setOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)), days)
    }

    @Test
    fun `a 3-day all-day event maps to three dates`() {
        val instance = EventInstance(
            id = 4,
            title = "Conference",
            start = ZonedDateTime.of(2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC),
            end = ZonedDateTime.of(2026, 9, 13, 0, 0, 0, 0, ZoneOffset.UTC),
            allDay = true,
        )

        val days = InstanceMapper.eventDays(listOf(instance), madrid)

        assertEquals(
            setOf(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12)),
            days,
        )
    }

    @Test
    fun `eventDays does not filter by window - that is the caller's responsibility`() {
        val outsideAnyGridWindow = EventInstance(
            id = 5,
            title = "Far future",
            start = ZonedDateTime.of(2099, 1, 1, 10, 0, 0, 0, madrid),
            end = ZonedDateTime.of(2099, 1, 1, 11, 0, 0, 0, madrid),
            allDay = false,
        )

        val days = InstanceMapper.eventDays(listOf(outsideAnyGridWindow), madrid)

        assertEquals(setOf(LocalDate.of(2099, 1, 1)), days)
    }
}
