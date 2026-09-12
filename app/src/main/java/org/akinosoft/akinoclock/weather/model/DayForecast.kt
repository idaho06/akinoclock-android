package org.akinosoft.akinoclock.weather.model

import java.time.LocalDate

data class DayForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val minC: Double,
    val maxC: Double,
)
