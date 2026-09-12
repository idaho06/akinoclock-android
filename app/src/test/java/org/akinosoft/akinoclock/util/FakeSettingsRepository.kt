package org.akinosoft.akinoclock.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.akinosoft.akinoclock.weather.model.WeatherLocation

class FakeSettingsRepository(
    initialFeeds: List<FeedConfig> = emptyList(),
    initialTheme: ThemeMode = ThemeMode.SYSTEM,
    initialWeatherLocation: WeatherLocation? = null,
) : SettingsRepository {
    private val feedsFlow = MutableStateFlow(initialFeeds)
    private val themeFlow = MutableStateFlow(initialTheme)
    private val weatherLocationFlow = MutableStateFlow(initialWeatherLocation)
    private var permissionAskedValue = false

    override val feeds: Flow<List<FeedConfig>> = feedsFlow
    override val themeMode: Flow<ThemeMode> = themeFlow
    override val weatherLocation: Flow<WeatherLocation?> = weatherLocationFlow

    override suspend fun setFeeds(list: List<FeedConfig>) {
        feedsFlow.value = list
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeFlow.value = mode
    }

    override suspend fun setWeatherLocation(location: WeatherLocation?) {
        weatherLocationFlow.value = location
    }

    override fun currentFeeds(): List<FeedConfig> = feedsFlow.value
    override fun currentThemeMode(): ThemeMode = themeFlow.value
    override fun currentWeatherLocation(): WeatherLocation? = weatherLocationFlow.value
    override fun permissionAsked(): Boolean = permissionAskedValue
    override fun setPermissionAsked() {
        permissionAskedValue = true
    }
}
