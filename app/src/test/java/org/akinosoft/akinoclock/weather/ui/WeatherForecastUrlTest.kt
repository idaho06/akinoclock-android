package org.akinosoft.akinoclock.weather.ui

import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherForecastUrlTest {

    @Test
    fun `builds a noadsweather forecast url from latitude, longitude and the encoded name`() {
        val location = WeatherLocation(name = "Madrid, Madrid, Spain", latitude = 40.4165, longitude = -3.7026)

        val url = WeatherForecastUrl.build(location)

        assertEquals(
            "https://noadsweather.com/?lat=40.4165&lon=-3.7026&name=Madrid%2C+Madrid%2C+Spain",
            url,
        )
    }
}
