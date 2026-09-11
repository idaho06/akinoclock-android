package org.akinosoft.akinoclock.settings.ui

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.logic.FeedUrlValidator
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.akinosoft.akinoclock.util.runViewModelTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val feedA = FeedConfig(url = "https://example.com/a.xml")
    private val feedB = FeedConfig(url = "https://example.com/b.xml")

    private class FakeSettingsRepository(
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

    private fun rssRepository(outcome: RefreshOutcome = RefreshOutcome.SUCCESS): RssRepository {
        val repo = mockk<RssRepository>()
        coEvery { repo.refresh(any()) } returns outcome
        return repo
    }

    @Test
    fun `feeds exposes the repository's current feeds`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        assertEquals(listOf(feedA), viewModel.feeds.value)
    }

    @Test
    fun `themeMode exposes the repository's current theme`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialTheme = ThemeMode.DARK)
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `addFeed with a valid new URL persists it`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        val result = viewModel.addFeed("example.com/b")
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Valid)
        assertEquals(listOf(feedA, FeedConfig(url = "https://example.com/b")), settingsRepository.currentFeeds())
    }

    @Test
    fun `addFeed with an invalid URL does not persist`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        val result = viewModel.addFeed("ftp://bad")
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Invalid)
        assertEquals(listOf(feedA), settingsRepository.currentFeeds())
    }

    @Test
    fun `addFeed with a duplicate URL does not persist`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        val result = viewModel.addFeed(feedA.url)
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Invalid)
        assertEquals(listOf(feedA), settingsRepository.currentFeeds())
    }

    @Test
    fun `removeFeed removes the given url`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA, feedB))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        viewModel.removeFeed(feedA.url)
        advanceUntilIdle()

        assertEquals(listOf(feedB), settingsRepository.currentFeeds())
    }

    @Test
    fun `updateFeed replaces the old url with the validated new url`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA, feedB))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        val result = viewModel.updateFeed(feedA.url, "example.com/edited")
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Valid)
        assertEquals(
            listOf(FeedConfig(url = "https://example.com/edited"), feedB),
            settingsRepository.currentFeeds(),
        )
    }

    @Test
    fun `updateFeed to its own current URL does not count as a duplicate`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) {}

        val result = viewModel.updateFeed(feedA.url, feedA.url)
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Valid)
        assertEquals(listOf(feedA), settingsRepository.currentFeeds())
    }

    @Test
    fun `setTheme persists via the repository and invokes the theme applier`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository()
        val applied = mutableListOf<ThemeMode>()
        val viewModel = SettingsViewModel(settingsRepository, rssRepository()) { applied += it }

        viewModel.setTheme(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, settingsRepository.currentThemeMode())
        assertEquals(listOf(ThemeMode.DARK), applied)
    }

    @Test
    fun `refreshNow calls RssRepository refresh with the current feeds and emits its outcome`() =
        runViewModelTest {
            val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
            val rss = rssRepository(RefreshOutcome.PARTIAL_FAILURE)
            val viewModel = SettingsViewModel(settingsRepository, rss) {}
            val results = mutableListOf<RefreshOutcome>()
            val job = launch { viewModel.refreshResult.toList(results) }

            viewModel.refreshNow()
            advanceUntilIdle()

            coVerify { rss.refresh(listOf(feedA)) }
            assertEquals(listOf(RefreshOutcome.PARTIAL_FAILURE), results)
            job.cancel()
        }

    @Test
    fun `the Factory creates a SettingsViewModel`() {
        val settingsRepository = FakeSettingsRepository()
        val factory = SettingsViewModel.Factory(settingsRepository, rssRepository()) {}

        val viewModel = factory.create(SettingsViewModel::class.java)

        assertEquals(emptyList<FeedConfig>(), viewModel.feeds.value)
    }
}
