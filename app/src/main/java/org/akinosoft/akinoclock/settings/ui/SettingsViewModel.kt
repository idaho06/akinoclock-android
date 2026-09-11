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

/**
 * [refreshNow] bypasses [RssViewModel][org.akinosoft.akinoclock.rss.ui.RssViewModel] and calls
 * [RssRepository.refresh] directly, so "Refresh now" works even if `MainActivity` isn't alive.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val rssRepository: RssRepository,
    private val applyTheme: (ThemeMode) -> Unit,
) : ViewModel() {

    val feeds: StateFlow<List<FeedConfig>> = settingsRepository.feeds
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.currentFeeds())

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.currentThemeMode())

    private val _refreshResult = MutableSharedFlow<RefreshOutcome>(replay = 0, extraBufferCapacity = 1)
    val refreshResult: SharedFlow<RefreshOutcome> = _refreshResult.asSharedFlow()

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

    fun refreshNow() {
        viewModelScope.launch {
            val outcome = rssRepository.refresh(feeds.value)
            _refreshResult.tryEmit(outcome)
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
        private val applyTheme: (ThemeMode) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository, rssRepository, applyTheme) as T
    }
}
