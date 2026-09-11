package org.akinosoft.akinoclock.settings.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.model.DefaultFeeds
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SharedPreferencesSettingsRepositoryTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `feeds emits the defaults when the feeds key was never set`() = runTest {
        val repo = SharedPreferencesSettingsRepository(context())

        assertEquals(DefaultFeeds.list, repo.currentFeeds())
    }

    @Test
    fun `feeds emits an empty list after setFeeds(emptyList()), without re-seeding`() = runTest {
        val repo = SharedPreferencesSettingsRepository(context())

        repo.setFeeds(emptyList())

        assertEquals(emptyList<FeedConfig>(), repo.currentFeeds())
    }

    @Test
    fun `a second instance over the same prefs sees a prior setFeeds`() = runTest {
        val feed = FeedConfig(url = "https://example.com/a.xml", title = "A")
        SharedPreferencesSettingsRepository(context()).setFeeds(listOf(feed))

        val second = SharedPreferencesSettingsRepository(context())

        assertEquals(listOf(feed), second.currentFeeds())
    }

    @Test
    fun `themeMode defaults to SYSTEM`() = runTest {
        val repo = SharedPreferencesSettingsRepository(context())

        assertEquals(ThemeMode.SYSTEM, repo.currentThemeMode())
    }

    @Test
    fun `a second instance over the same prefs sees a prior setThemeMode`() = runTest {
        SharedPreferencesSettingsRepository(context()).setThemeMode(ThemeMode.DARK)

        val second = SharedPreferencesSettingsRepository(context())

        assertEquals(ThemeMode.DARK, second.currentThemeMode())
    }

    @Test
    fun `feeds flow emits the current value to an active collector on setFeeds`() = runTest(UnconfinedTestDispatcher()) {
        val repo = SharedPreferencesSettingsRepository(context(), UnconfinedTestDispatcher(testScheduler))
        val collected = mutableListOf<List<FeedConfig>>()
        val job = launch { repo.feeds.toList(collected) }
        val newFeeds = listOf(FeedConfig(url = "https://example.com/new.xml"))

        repo.setFeeds(newFeeds)

        assertEquals(DefaultFeeds.list, collected.first())
        assertEquals(newFeeds, collected.last())
        job.cancel()
    }

    @Test
    fun `themeMode flow emits the current value to an active collector on setThemeMode`() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = SharedPreferencesSettingsRepository(context(), UnconfinedTestDispatcher(testScheduler))
            val collected = mutableListOf<ThemeMode>()
            val job = launch { repo.themeMode.toList(collected) }

            repo.setThemeMode(ThemeMode.LIGHT)

            assertEquals(ThemeMode.SYSTEM, collected.first())
            assertEquals(ThemeMode.LIGHT, collected.last())
            job.cancel()
        }

    @Test
    fun `the listener is unregistered when the flow collector cancels`() = runTest(UnconfinedTestDispatcher()) {
        val repo = SharedPreferencesSettingsRepository(context(), UnconfinedTestDispatcher(testScheduler))
        val collected = mutableListOf<List<FeedConfig>>()
        val job = launch { repo.feeds.toList(collected) }
        job.cancel()
        job.join()

        repo.setFeeds(listOf(FeedConfig(url = "https://example.com/after-cancel.xml")))

        assertEquals(1, collected.size)
    }

    @Test
    fun `permissionAsked defaults to false`() {
        val repo = SharedPreferencesSettingsRepository(context())

        assertFalse(repo.permissionAsked())
    }

    @Test
    fun `setPermissionAsked persists true`() {
        val repo = SharedPreferencesSettingsRepository(context())

        repo.setPermissionAsked()

        assertTrue(repo.permissionAsked())
    }

    @Test
    fun `a second instance over the same prefs sees a prior setPermissionAsked`() {
        SharedPreferencesSettingsRepository(context()).setPermissionAsked()

        val second = SharedPreferencesSettingsRepository(context())

        assertTrue(second.permissionAsked())
    }
}
