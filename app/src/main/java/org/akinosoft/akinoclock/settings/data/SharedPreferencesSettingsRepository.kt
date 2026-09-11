package org.akinosoft.akinoclock.settings.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.model.DefaultFeeds
import org.akinosoft.akinoclock.settings.model.FeedsJson
import org.akinosoft.akinoclock.settings.model.ThemeMode

class SharedPreferencesSettingsRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val feeds: Flow<List<FeedConfig>> = preferenceChanges(KEY_FEEDS, ::currentFeeds).distinctUntilChanged()

    override val themeMode: Flow<ThemeMode> =
        preferenceChanges(KEY_THEME_MODE, ::currentThemeMode).distinctUntilChanged()

    override suspend fun setFeeds(list: List<FeedConfig>) {
        withContext(ioDispatcher) {
            prefs.edit().putString(KEY_FEEDS, FeedsJson.encode(list)).commit()
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        withContext(ioDispatcher) {
            prefs.edit().putString(KEY_THEME_MODE, mode.storageValue).commit()
        }
    }

    override fun currentFeeds(): List<FeedConfig> {
        val stored = prefs.getString(KEY_FEEDS, null) ?: return DefaultFeeds.list
        return FeedsJson.decode(stored)
    }

    override fun currentThemeMode(): ThemeMode = ThemeMode.fromStorageString(prefs.getString(KEY_THEME_MODE, null))

    override fun permissionAsked(): Boolean = prefs.getBoolean(KEY_PERMISSION_ASKED, false)

    override fun setPermissionAsked() {
        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
    }

    /** [awaitClose] unregisters the listener when the collecting coroutine is cancelled; the
     * listener itself is kept alive as a local val for the coroutine's lifetime, since
     * [SharedPreferences] only holds it weakly. */
    private fun <T> preferenceChanges(key: String, currentValue: () -> T): Flow<T> = callbackFlow {
        trySend(currentValue())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) trySend(currentValue())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val PREFS_NAME = "akinoclock_settings"
        private const val KEY_FEEDS = "feeds"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PERMISSION_ASKED = "calendar_permission_asked"
    }
}
