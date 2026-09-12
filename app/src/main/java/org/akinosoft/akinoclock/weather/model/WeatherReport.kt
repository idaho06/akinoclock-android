package org.akinosoft.akinoclock.weather.model

import java.time.Instant

/** [days] always has exactly 3 entries (today + 2); [days]'s first entry is today's min/max. */
data class WeatherReport(
    val current: CurrentWeather,
    val days: List<DayForecast>,
    val fetchedAt: Instant,
)
