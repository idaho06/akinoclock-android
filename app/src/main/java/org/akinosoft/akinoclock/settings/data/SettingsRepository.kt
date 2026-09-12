package org.akinosoft.akinoclock.settings.data

import kotlinx.coroutines.flow.Flow
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.akinosoft.akinoclock.weather.model.WeatherLocation

interface SettingsRepository {
    val feeds: Flow<List<FeedConfig>>
    val themeMode: Flow<ThemeMode>
    val weatherLocation: Flow<WeatherLocation?>

    suspend fun setFeeds(list: List<FeedConfig>)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setWeatherLocation(location: WeatherLocation?)

    fun currentFeeds(): List<FeedConfig>
    fun currentThemeMode(): ThemeMode
    fun currentWeatherLocation(): WeatherLocation?

    fun permissionAsked(): Boolean
    fun setPermissionAsked()
}
