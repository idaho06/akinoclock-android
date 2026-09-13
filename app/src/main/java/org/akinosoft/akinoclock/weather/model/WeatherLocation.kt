package org.akinosoft.akinoclock.weather.model

data class WeatherLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Full English country name (e.g. "Spain"), as returned by Open-Meteo's geocoding API.
     * Kept separately from [name] (which already folds it into a display label) so a forecast
     * link can pass it along for unit/time-format auto-detection. `null` for locations parsed
     * before this field existed. */
    val country: String? = null,
)
