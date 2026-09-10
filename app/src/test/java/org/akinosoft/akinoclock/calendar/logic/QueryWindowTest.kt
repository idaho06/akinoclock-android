package org.akinosoft.akinoclock.calendar.logic

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class QueryWindowTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    @Test
    fun `September 2026 window spans the Monday before the 1st to the Monday after the last grid cell, exclusive`() {
        val window = QueryWindow.forGrid(YearMonth.of(2026, 9), madrid)

        val expectedBegin = LocalDate.of(2026, 8, 31).atStartOfDay(madrid).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 10, 12).atStartOfDay(madrid).toInstant().toEpochMilli()

        assertEquals(expectedBegin, window.beginMillis)
        assertEquals(expectedEnd, window.endMillis)
        // guard against zone mistakes: 2026-08-31T00:00+02:00 and 2026-10-12T00:00+02:00
        assertEquals(1788127200000L, window.beginMillis)
        assertEquals(1791756000000L, window.endMillis)
    }
}
