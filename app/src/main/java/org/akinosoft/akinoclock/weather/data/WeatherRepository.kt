package org.akinosoft.akinoclock.weather.data

import kotlinx.coroutines.flow.Flow
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.akinosoft.akinoclock.weather.model.WeatherReport

interface WeatherRepository {
    fun report(): Flow<WeatherReport?>
    fun primeFromCache(location: WeatherLocation)
    suspend fun refresh(location: WeatherLocation): WeatherRefreshOutcome
}
