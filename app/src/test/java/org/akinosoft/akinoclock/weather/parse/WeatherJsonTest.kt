package org.akinosoft.akinoclock.weather.parse

import java.time.Instant
import java.time.LocalDate
import org.akinosoft.akinoclock.weather.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class WeatherJsonTest {

    private fun fixture(name: String) =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("weather/$name")) { "missing fixture $name" }
            .bufferedReader().readText()

    private val fetchedAt = Instant.parse("2026-09-12T10:00:00Z")

    @Test
    fun `parses the madrid forecast fixture`() {
        val report = WeatherJson.parse(fixture("forecast-madrid.json"), fetchedAt)

        assertEquals(20.1, report.current.temperatureC, 0.0)
        assertEquals(WeatherCondition.CLEAR, report.current.condition)
        assertFalse(report.current.isDay)
        assertEquals(fetchedAt, report.fetchedAt)

        assertEquals(3, report.days.size)
        assertEquals(LocalDate.parse("2026-09-12"), report.days[0].date)
        assertEquals(LocalDate.parse("2026-09-13"), report.days[1].date)
        assertEquals(LocalDate.parse("2026-09-14"), report.days[2].date)
        report.days.forEach { assertEquals(WeatherCondition.PARTLY_CLOUDY, it.condition) }
        assertEquals(15.9, report.days[0].minC, 0.0)
        assertEquals(31.5, report.days[0].maxC, 0.0)
        assertEquals(17.1, report.days[1].minC, 0.0)
        assertEquals(32.1, report.days[1].maxC, 0.0)
        assertEquals(17.4, report.days[2].minC, 0.0)
        assertEquals(33.8, report.days[2].maxC, 0.0)
    }

    @Test
    fun `missing daily throws`() {
        val json = """{"current":{"temperature_2m":20.1,"weather_code":0,"is_day":1}}"""
        assertThrows(WeatherParseException::class.java) { WeatherJson.parse(json, fetchedAt) }
    }

    @Test
    fun `malformed json throws`() {
        assertThrows(WeatherParseException::class.java) { WeatherJson.parse(fixture("forecast-malformed.json"), fetchedAt) }
    }

    @Test
    fun `only two forecast days throws`() {
        val json = """
            {"current":{"temperature_2m":20.1,"weather_code":0,"is_day":1},
             "daily":{"time":["2026-09-12","2026-09-13"],"weather_code":[1,1],
                      "temperature_2m_max":[31.5,32.1],"temperature_2m_min":[15.9,17.1]}}
        """.trimIndent()
        assertThrows(WeatherParseException::class.java) { WeatherJson.parse(json, fetchedAt) }
    }
}
