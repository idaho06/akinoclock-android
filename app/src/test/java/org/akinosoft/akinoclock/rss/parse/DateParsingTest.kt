package org.akinosoft.akinoclock.rss.parse

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateParsingTest {

    @Test
    fun `parses RFC-1123 with GMT zone`() {
        assertEquals(
            Instant.parse("2026-09-10T08:00:00Z"),
            DateParsing.parse("Thu, 10 Sep 2026 08:00:00 GMT"),
        )
    }

    @Test
    fun `parses RFC-1123 with numeric offset`() {
        assertEquals(
            Instant.parse("2026-09-10T08:00:00Z"),
            DateParsing.parse("Thu, 10 Sep 2026 10:00:00 +0200"),
        )
    }

    @Test
    fun `parses ISO-8601 with Z suffix`() {
        assertEquals(Instant.parse("2026-09-10T08:00:00Z"), DateParsing.parse("2026-09-10T08:00:00Z"))
    }

    @Test
    fun `parses ISO-8601 with numeric offset`() {
        assertEquals(
            Instant.parse("2026-09-10T08:00:00Z"),
            DateParsing.parse("2026-09-10T10:00:00+02:00"),
        )
    }

    @Test
    fun `parses ISO-8601 with fractional seconds`() {
        assertEquals(
            Instant.parse("2026-09-10T08:00:00.123Z"),
            DateParsing.parse("2026-09-10T08:00:00.123Z"),
        )
    }

    @Test
    fun `returns null for garbage`() {
        assertNull(DateParsing.parse("not a date"))
        assertNull(DateParsing.parse(""))
    }
}
