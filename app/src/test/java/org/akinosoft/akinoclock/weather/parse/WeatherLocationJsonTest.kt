package org.akinosoft.akinoclock.weather.parse

import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherLocationJsonTest {

    @Test
    fun `round-trips a location`() {
        val location = WeatherLocation("Madrid, Madrid, Spain", 40.4165, -3.70256)
        assertEquals(location, WeatherLocationJson.decode(WeatherLocationJson.encode(location)))
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
