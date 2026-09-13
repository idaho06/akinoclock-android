package org.akinosoft.akinoclock.weather.parse

import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherLocationJsonTest {

    @Test
    fun `round-trips a location`() {
        val location = WeatherLocation("Madrid, Madrid, Spain", 40.4165, -3.70256, country = "Spain")
        assertEquals(location, WeatherLocationJson.decode(WeatherLocationJson.encode(location)))
    }

    @Test
    fun `a location with no country round-trips to a null country`() {
        val location = WeatherLocation("Nowhere", 1.0, 2.0, country = null)
        assertEquals(location, WeatherLocationJson.decode(WeatherLocationJson.encode(location)))
    }

    @Test
    fun `decoding a pre-existing preference with no country field yields a null country`() {
        val decoded = WeatherLocationJson.decode("""{"name":"Madrid, Spain","lat":40.4165,"lon":-3.70256}""")
        assertEquals(WeatherLocation("Madrid, Spain", 40.4165, -3.70256, country = null), decoded)
    }

    @Test
    fun `malformed json decodes to null`() {
        assertNull(WeatherLocationJson.decode("not json"))
    }

    @Test
    fun `empty string decodes to null`() {
        assertNull(WeatherLocationJson.decode(""))
    }
}
