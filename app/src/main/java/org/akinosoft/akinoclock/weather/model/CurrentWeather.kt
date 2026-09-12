package org.akinosoft.akinoclock.weather.model

data class CurrentWeather(
    val temperatureC: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
)
