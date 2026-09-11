package org.akinosoft.akinoclock.settings.data

import kotlinx.coroutines.flow.Flow
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.model.ThemeMode

interface SettingsRepository {
    val feeds: Flow<List<FeedConfig>>
    val themeMode: Flow<ThemeMode>

    suspend fun setFeeds(list: List<FeedConfig>)
    suspend fun setThemeMode(mode: ThemeMode)

    fun currentFeeds(): List<FeedConfig>
    fun currentThemeMode(): ThemeMode

    fun permissionAsked(): Boolean
    fun setPermissionAsked()
}
