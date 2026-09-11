package org.akinosoft.akinoclock.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.model.ThemeMode

class FakeSettingsRepository(
    initialFeeds: List<FeedConfig> = emptyList(),
    initialTheme: ThemeMode = ThemeMode.SYSTEM,
) : SettingsRepository {
    private val feedsFlow = MutableStateFlow(initialFeeds)
    private val themeFlow = MutableStateFlow(initialTheme)
    private var permissionAskedValue = false

    override val feeds: Flow<List<FeedConfig>> = feedsFlow
    override val themeMode: Flow<ThemeMode> = themeFlow

    override suspend fun setFeeds(list: List<FeedConfig>) {
        feedsFlow.value = list
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeFlow.value = mode
    }

    override fun currentFeeds(): List<FeedConfig> = feedsFlow.value
    override fun currentThemeMode(): ThemeMode = themeFlow.value
    override fun permissionAsked(): Boolean = permissionAskedValue
    override fun setPermissionAsked() {
        permissionAskedValue = true
    }
}
