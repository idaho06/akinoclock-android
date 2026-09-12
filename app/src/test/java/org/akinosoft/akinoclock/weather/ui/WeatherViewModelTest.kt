package org.akinosoft.akinoclock.weather.ui

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.akinosoft.akinoclock.util.runViewModelTest
import org.akinosoft.akinoclock.weather.data.WeatherRefreshOutcome
import org.akinosoft.akinoclock.weather.data.WeatherRepository
import org.akinosoft.akinoclock.weather.model.CurrentWeather
import org.akinosoft.akinoclock.weather.model.DayForecast
import org.akinosoft.akinoclock.weather.model.WeatherCondition
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.akinosoft.akinoclock.weather.model.WeatherReport
import org.akinosoft.akinoclock.weather.model.WeatherUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Mirrors [org.akinosoft.akinoclock.rss.ui.RssViewModelTest]'s approach to a never-idle
 * scheduling loop: `advanceTimeBy`/`runCurrent`, never `advanceUntilIdle()` while started. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WeatherViewModelTest {

    private val madrid = WeatherLocation("Madrid, Spain", 40.4168, -3.7038)
    private val paris = WeatherLocation("Paris, France", 48.8566, 2.3522)

    private fun report(temperatureC: Double = 20.0, fetchedAt: Instant = Instant.EPOCH) = WeatherReport(
        current = CurrentWeather(temperatureC, WeatherCondition.CLEAR, isDay = true),
        days = listOf(
            DayForecast(java.time.LocalDate.parse("2026-09-12"), WeatherCondition.CLEAR, 10.0, 20.0),
            DayForecast(java.time.LocalDate.parse("2026-09-13"), WeatherCondition.CLEAR, 10.0, 20.0),
            DayForecast(java.time.LocalDate.parse("2026-09-14"), WeatherCondition.CLEAR, 10.0, 20.0),
        ),
        fetchedAt = fetchedAt,
    )

    private class Repo(val reportFlow: MutableStateFlow<WeatherReport?> = MutableStateFlow(null)) {
        var outcome: WeatherRefreshOutcome = WeatherRefreshOutcome.SUCCESS
        val refreshCallTimes = mutableListOf<Long>()
        val refreshCallLocations = mutableListOf<WeatherLocation>()
        val primedLocations = mutableListOf<WeatherLocation>()
    }

    private fun TestScope.mockRepository(repo: Repo = Repo()): WeatherRepository = mockk {
        every { report() } returns repo.reportFlow
        every { primeFromCache(any()) } answers { repo.primedLocations += firstArg<WeatherLocation>() }
        coEvery { refresh(any()) } coAnswers {
            repo.refreshCallTimes += testScheduler.currentTime
            repo.refreshCallLocations += firstArg<WeatherLocation>()
            repo.outcome
        }
    }

    @Test
    fun `no location yields NoLocation and never calls the repository`() = runViewModelTest {
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(null), Clock.systemUTC())

        viewModel.start()
        advanceTimeBy(2 * 60 * 60_000L + 1)
        runCurrent()

        assertEquals(WeatherUiState.NoLocation, viewModel.uiState.value)
        assertEquals(0, repo.refreshCallTimes.size)
    }

    @Test
    fun `start triggers an immediate refresh when a location is set`() = runViewModelTest {
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())

        viewModel.start()
        runCurrent()

        assertEquals(1, repo.refreshCallTimes.size)
        assertEquals(listOf(madrid), repo.refreshCallLocations)
    }

    @Test
    fun `refreshes every 60 minutes while started, and stop cancels further refreshes`() = runViewModelTest {
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())

        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)

        advanceTimeBy(60 * 60_000L + 1)
        runCurrent()
        assertEquals(2, repo.refreshCallTimes.size)

        viewModel.stop()
        advanceTimeBy(2 * 60 * 60_000L)
        advanceUntilIdle()
        assertEquals(2, repo.refreshCallTimes.size)
    }

    @Test
    fun `start again within 10 minutes of a success does not refresh`() = runViewModelTest {
        val repo = Repo()
        var now = Instant.parse("2026-09-12T12:00:00Z")
        val clock = object : Clock() {
            override fun instant() = now
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
        }
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), clock)
        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)
        viewModel.stop()

        now = now.plusSeconds(5 * 60)
        viewModel.start()
        runCurrent()

        assertEquals(1, repo.refreshCallTimes.size)
    }

    @Test
    fun `start again after 10 minutes of a success refreshes immediately`() = runViewModelTest {
        val repo = Repo()
        var now = Instant.parse("2026-09-12T12:00:00Z")
        val clock = object : Clock() {
            override fun instant() = now
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
        }
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), clock)
        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)
        viewModel.stop()

        now = now.plusSeconds(11 * 60)
        viewModel.start()
        runCurrent()

        assertEquals(2, repo.refreshCallTimes.size)
    }

    @Test
    fun `failures retry with backoff of 1, 2, 4, 8, 15, 15 minutes, and success resets it`() = runViewModelTest {
        val repo = Repo()
        repo.outcome = WeatherRefreshOutcome.FAILED
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())

        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)

        val expectedBackoffMinutes = listOf(1L, 2L, 4L, 8L, 15L, 15L)
        for (minutes in expectedBackoffMinutes) {
            advanceTimeBy(minutes * 60_000L + 1)
            runCurrent()
        }
        assertEquals(1 + expectedBackoffMinutes.size, repo.refreshCallTimes.size)

        repo.outcome = WeatherRefreshOutcome.SUCCESS
        advanceTimeBy(15 * 60_000L + 1)
        runCurrent()
        val successCallIndex = repo.refreshCallTimes.size - 1

        advanceTimeBy(60 * 60_000L + 1)
        runCurrent()

        assertEquals(successCallIndex + 2, repo.refreshCallTimes.size)
    }

    @Test
    fun `a location change mid-delay refreshes immediately for the new location`() = runViewModelTest {
        val repo = Repo()
        val locationFlow = MutableStateFlow(madrid)
        val viewModel = WeatherViewModel(mockRepository(repo), locationFlow, Clock.systemUTC())
        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)

        locationFlow.value = paris
        runCurrent()

        assertEquals(2, repo.refreshCallTimes.size)
        assertEquals(paris, repo.refreshCallLocations.last())
    }

    @Test
    fun `refreshNow refreshes immediately regardless of the schedule`() = runViewModelTest {
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())

        viewModel.refreshNow()
        advanceUntilIdle()

        assertEquals(1, repo.refreshCallTimes.size)
        assertEquals(listOf(madrid), repo.refreshCallLocations)
    }

    @Test
    fun `Showing reflects the current report and is not stale right after a success`() = runViewModelTest {
        val fixedNow = Instant.parse("2026-09-12T12:00:00Z")
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), clock)
        viewModel.start()
        runCurrent()
        repo.reportFlow.value = report(fetchedAt = fixedNow)
        runCurrent()

        val state = viewModel.uiState.value as WeatherUiState.Showing
        assertTrue(!state.stale)
        assertEquals(20.0, state.report.current.temperatureC, 0.0)
    }

    @Test
    fun `Showing is stale once the report's fetchedAt is older than 2 hours`() = runViewModelTest {
        val fixedNow = Instant.parse("2026-09-12T12:00:00Z")
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), clock)
        viewModel.start()
        runCurrent()

        repo.reportFlow.value = report(fetchedAt = fixedNow.minus(java.time.Duration.ofHours(2).plusSeconds(1)))
        runCurrent()

        val state = viewModel.uiState.value as WeatherUiState.Showing
        assertTrue(state.stale)
    }

    @Test
    fun `Showing is stale while the latest attempt is failing, even with a report present`() = runViewModelTest {
        val repo = Repo()
        repo.outcome = WeatherRefreshOutcome.FAILED
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())
        viewModel.start()
        runCurrent()
        // A report must exist for the state to be Showing at all (no report yet is Loading) —
        // the repository would normally already hold one from an earlier success.
        repo.reportFlow.value = report(fetchedAt = Instant.now())
        runCurrent()

        val state = viewModel.uiState.value as WeatherUiState.Showing
        assertTrue(state.stale)
    }

    @Test
    fun `no report yet is Loading, not NoLocation`() = runViewModelTest {
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())

        viewModel.start()
        runCurrent()

        assertEquals(WeatherUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `primeFromCache is called for the initial location on start`() = runViewModelTest {
        val repo = Repo()
        val viewModel = WeatherViewModel(mockRepository(repo), MutableStateFlow(madrid), Clock.systemUTC())

        viewModel.start()
        runCurrent()

        assertEquals(listOf(madrid), repo.primedLocations)
    }
}
