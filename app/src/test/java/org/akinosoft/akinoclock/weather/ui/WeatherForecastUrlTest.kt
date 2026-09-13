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

    @Test
    fun `includes the country so the site auto-detects metric units and 24h time instead of guessing from the browser locale`() {
        val location = WeatherLocation(name = "Madrid, Spain", latitude = 40.4165, longitude = -3.7026, country = "Spain")

        val url = WeatherForecastUrl.build(location)

        assertEquals(
            "https://noadsweather.com/?lat=40.4165&lon=-3.7026&name=Madrid%2C+Spain&country=Spain",
            url,
        )
    }

    @Test
    fun `omits the country param entirely when the location has none`() {
        val location = WeatherLocation(name = "Somewhere", latitude = 1.0, longitude = 2.0, country = null)

        val url = WeatherForecastUrl.build(location)

        assertEquals("https://noadsweather.com/?lat=1.0&lon=2.0&name=Somewhere", url)
    }
}
