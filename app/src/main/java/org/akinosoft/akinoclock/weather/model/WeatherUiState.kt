package org.akinosoft.akinoclock.weather.model

sealed class WeatherUiState {
    data object NoLocation : WeatherUiState()
    data object Loading : WeatherUiState()
    data class Showing(val report: WeatherReport, val location: WeatherLocation, val stale: Boolean) : WeatherUiState()
}
