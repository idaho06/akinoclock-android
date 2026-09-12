package org.akinosoft.akinoclock.settings.ui

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.logic.FeedUrlValidator
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.akinosoft.akinoclock.util.FakeSettingsRepository
import org.akinosoft.akinoclock.util.runViewModelTest
import org.akinosoft.akinoclock.weather.data.GeocodingClient
import org.akinosoft.akinoclock.weather.data.GeocodingResult
import org.akinosoft.akinoclock.weather.data.WeatherRefreshOutcome
import org.akinosoft.akinoclock.weather.data.WeatherRepository
import org.akinosoft.akinoclock.weather.model.WeatherLocation
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

    private fun rssRepository(outcome: RefreshOutcome = RefreshOutcome.SUCCESS): RssRepository {
        val repo = mockk<RssRepository>()
        coEvery { repo.refresh(any()) } returns outcome
        return repo
    }

    private fun weatherRepository(outcome: WeatherRefreshOutcome = WeatherRefreshOutcome.SUCCESS): WeatherRepository {
        val repo = mockk<WeatherRepository>()
        coEvery { repo.refresh(any()) } returns outcome
        return repo
    }

    private fun geocodingClient(result: GeocodingResult = GeocodingResult.NoResults): GeocodingClient {
        val client = mockk<GeocodingClient>()
        coEvery { client.search(any()) } returns result
        return client
    }

    @Test
    fun `feeds exposes the repository's current feeds`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        assertEquals(listOf(feedA), viewModel.feeds.value)
    }

    @Test
    fun `themeMode exposes the repository's current theme`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialTheme = ThemeMode.DARK)
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `addFeed with a valid new URL persists it`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        val result = viewModel.addFeed("example.com/b")
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Valid)
        assertEquals(listOf(feedA, FeedConfig(url = "https://example.com/b")), settingsRepository.currentFeeds())
    }

    @Test
    fun `addFeed with an invalid URL does not persist`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        val result = viewModel.addFeed("ftp://bad")
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Invalid)
        assertEquals(listOf(feedA), settingsRepository.currentFeeds())
    }

    @Test
    fun `addFeed with a duplicate URL does not persist`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        val result = viewModel.addFeed(feedA.url)
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Invalid)
        assertEquals(listOf(feedA), settingsRepository.currentFeeds())
    }

    @Test
    fun `removeFeed removes the given url`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA, feedB))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        viewModel.removeFeed(feedA.url)
        advanceUntilIdle()

        assertEquals(listOf(feedB), settingsRepository.currentFeeds())
    }

    @Test
    fun `updateFeed replaces the old url with the validated new url`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository(initialFeeds = listOf(feedA, feedB))
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

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
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        val result = viewModel.updateFeed(feedA.url, feedA.url)
        advanceUntilIdle()

        assertTrue(result is FeedUrlValidator.Result.Valid)
        assertEquals(listOf(feedA), settingsRepository.currentFeeds())
    }

    @Test
    fun `setTheme persists via the repository and invokes the theme applier`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository()
        val applied = mutableListOf<ThemeMode>()
        val viewModel =
            SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) { applied += it }

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
            val viewModel = SettingsViewModel(settingsRepository, rss, weatherRepository(), geocodingClient()) {}
            val results = mutableListOf<RefreshOutcome>()
            val job = launch { viewModel.refreshResult.toList(results) }

            viewModel.refreshNow()
            advanceUntilIdle()

            coVerify { rss.refresh(listOf(feedA)) }
            assertEquals(listOf(RefreshOutcome.PARTIAL_FAILURE), results)
            job.cancel()
        }

    @Test
    fun `refreshNow also refreshes weather when a location is set`() = runViewModelTest {
        val madrid = WeatherLocation("Madrid, Spain", 40.4165, -3.70256)
        val settingsRepository = FakeSettingsRepository(initialWeatherLocation = madrid)
        val weather = weatherRepository()
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weather, geocodingClient()) {}

        viewModel.refreshNow()
        advanceUntilIdle()

        coVerify { weather.refresh(madrid) }
    }

    @Test
    fun `refreshNow does not touch the weather repository when no location is set`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository()
        val weather = weatherRepository()
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weather, geocodingClient()) {}

        viewModel.refreshNow()
        advanceUntilIdle()

        coVerify(exactly = 0) { weather.refresh(any()) }
    }

    @Test
    fun `searchLocation with a blank query never hits the client`() = runViewModelTest {
        val settingsRepository = FakeSettingsRepository()
        val geocoding = geocodingClient()
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocoding) {}

        viewModel.searchLocation("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { geocoding.search(any()) }
    }

    @Test
    fun `searchLocation emits the client's result`() = runViewModelTest {
        val found = GeocodingResult.Found(listOf(WeatherLocation("Madrid, Spain", 40.4165, -3.70256)))
        val settingsRepository = FakeSettingsRepository()
        val viewModel =
            SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient(found)) {}
        val results = mutableListOf<GeocodingResult>()
        val job = launch { viewModel.searchResult.toList(results) }

        viewModel.searchLocation("Madrid")
        advanceUntilIdle()

        assertEquals(listOf(found), results)
        job.cancel()
    }

    @Test
    fun `setWeatherLocation persists the location`() = runViewModelTest {
        val madrid = WeatherLocation("Madrid, Spain", 40.4165, -3.70256)
        val settingsRepository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        viewModel.setWeatherLocation(madrid)
        advanceUntilIdle()

        assertEquals(madrid, settingsRepository.currentWeatherLocation())
    }

    @Test
    fun `the Factory creates a SettingsViewModel`() {
        val settingsRepository = FakeSettingsRepository()
        val factory =
            SettingsViewModel.Factory(settingsRepository, rssRepository(), weatherRepository(), geocodingClient()) {}

        val viewModel = factory.create(SettingsViewModel::class.java)

        assertEquals(emptyList<FeedConfig>(), viewModel.feeds.value)
    }
}
