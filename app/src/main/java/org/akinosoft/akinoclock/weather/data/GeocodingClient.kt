package org.akinosoft.akinoclock.weather.data

import org.akinosoft.akinoclock.weather.model.WeatherLocation

sealed class GeocodingResult {
    data class Found(val locations: List<WeatherLocation>) : GeocodingResult()
    data object NoResults : GeocodingResult()
    data object Failed : GeocodingResult()
}

interface GeocodingClient {
    suspend fun search(query: String): GeocodingResult
}
