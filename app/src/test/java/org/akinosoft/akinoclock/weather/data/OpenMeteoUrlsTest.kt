package org.akinosoft.akinoclock.weather.data

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoUrlsTest {

    @After
    fun resetLocale() {
        Locale.setDefault(Locale.ROOT)
    }

    @Test
    fun `builds the exact forecast url`() {
        val url = OpenMeteoUrls.forecast(latitude = 40.4168, longitude = -3.7038, timeZoneId = "Europe/Madrid")

        assertEquals(
            "https://api.open-meteo.com/v1/forecast?latitude=40.4168&longitude=-3.7038" +
                "&current=temperature_2m,weather_code,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&timezone=Europe%2FMadrid&forecast_days=3",
            url,
        )
    }

    @Test
    fun `forecast url formats numbers with a dot regardless of device locale`() {
        Locale.setDefault(Locale("es", "ES"))

        val url = OpenMeteoUrls.forecast(latitude = 40.4168, longitude = -3.7038, timeZoneId = "Europe/Madrid")

        assertEquals(true, url.contains("latitude=40.4168&longitude=-3.7038"))
    }

    @Test
    fun `builds the exact geocoding url`() {
        val url = OpenMeteoUrls.geocoding("San Sebastián")

        assertEquals(
            "https://geocoding-api.open-meteo.com/v1/search?name=San+Sebasti%C3%A1n&count=5&language=en&format=json",
            url,
        )
    }
}
