package org.akinosoft.akinoclock.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.logic.FeedUrlValidator
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.akinosoft.akinoclock.weather.data.GeocodingClient
import org.akinosoft.akinoclock.weather.data.GeocodingResult
import org.akinosoft.akinoclock.weather.data.WeatherRepository
import org.akinosoft.akinoclock.weather.model.WeatherLocation

/**
 * [refreshNow] bypasses [RssViewModel][org.akinosoft.akinoclock.rss.ui.RssViewModel] and calls
 * [RssRepository.refresh] directly, so "Refresh now" works even if `MainActivity` isn't alive; it
 * does the same for [WeatherRepository] when a location is set, but its outcome isn't surfaced —
 * the "Refresh now" Toast wording stays feed-only.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val rssRepository: RssRepository,
    private val weatherRepository: WeatherRepository,
    private val geocodingClient: GeocodingClient,
    private val applyTheme: (ThemeMode) -> Unit,
) : ViewModel() {

    val feeds: StateFlow<List<FeedConfig>> = settingsRepository.feeds
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.currentFeeds())

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.currentThemeMode())

    val weatherLocation: StateFlow<WeatherLocation?> = settingsRepository.weatherLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.currentWeatherLocation())

    private val _refreshResult = MutableSharedFlow<RefreshOutcome>(replay = 0, extraBufferCapacity = 1)
    val refreshResult: SharedFlow<RefreshOutcome> = _refreshResult.asSharedFlow()

    private val _searchResult = MutableSharedFlow<GeocodingResult>(replay = 0, extraBufferCapacity = 1)
    val searchResult: SharedFlow<GeocodingResult> = _searchResult.asSharedFlow()

    fun addFeed(url: String): FeedUrlValidator.Result =
        validateAndPersist(url) { validUrl -> feeds.value + FeedConfig(url = validUrl) }

    fun updateFeed(oldUrl: String, newUrl: String): FeedUrlValidator.Result =
        validateAndPersist(newUrl, excluding = oldUrl) { validUrl ->
            feeds.value.map { if (it.url == oldUrl) it.copy(url = validUrl) else it }
        }

    fun removeFeed(url: String) {
        persist(feeds.value.filterNot { it.url == url })
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
        applyTheme(mode)
    }

    fun setWeatherLocation(location: WeatherLocation?) {
        viewModelScope.launch { settingsRepository.setWeatherLocation(location) }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch { _searchResult.tryEmit(geocodingClient.search(query)) }
    }

    fun refreshNow() {
        viewModelScope.launch {
            val outcome = rssRepository.refresh(feeds.value)
            _refreshResult.tryEmit(outcome)
        }
        weatherLocation.value?.let { location ->
            viewModelScope.launch { weatherRepository.refresh(location) }
        }
    }

    private fun validateAndPersist(
        url: String,
        excluding: String? = null,
        newFeeds: (validUrl: String) -> List<FeedConfig>,
    ): FeedUrlValidator.Result {
        val result = FeedUrlValidator.validate(url, feeds.value, excluding)
        if (result is FeedUrlValidator.Result.Valid) persist(newFeeds(result.url))
        return result
    }

    private fun persist(list: List<FeedConfig>) {
        viewModelScope.launch { settingsRepository.setFeeds(list) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val rssRepository: RssRepository,
        private val weatherRepository: WeatherRepository,
        private val geocodingClient: GeocodingClient,
        private val applyTheme: (ThemeMode) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository, rssRepository, weatherRepository, geocodingClient, applyTheme) as T
    }
}
