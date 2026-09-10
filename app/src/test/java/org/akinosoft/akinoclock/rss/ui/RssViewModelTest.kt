package org.akinosoft.akinoclock.rss.ui

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.rss.model.FeedStatus
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.rss.model.RssUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [RssViewModel.start] launches a scheduling loop that reschedules itself forever
 * (`while (true) { delay(...) }`), so tests here use [runCurrent]/[advanceTimeBy] to move
 * virtual time by bounded amounts. Calling `advanceUntilIdle()` once that loop is running would
 * spin forever, since the scheduler always has another future delay queued.
 *
 * Runs under Robolectric (not plain JUnit) for the same reason as `CalendarViewModelTest`:
 * `viewModelScope`/`Dispatchers.setMain()` need a working `Looper.getMainLooper()` to resolve
 * the platform Main dispatcher, which is unmocked on the plain JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RssViewModelTest {

    private val feedA = FeedConfig(url = "https://example.com/a.xml")
    private val feedB = FeedConfig(url = "https://example.com/b.xml")

    private fun headline(title: String) = Headline(feedTitle = "feed", title = title, link = null, published = null)

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class Repo(
        val headlinesFlow: MutableStateFlow<List<Headline>> = MutableStateFlow(emptyList()),
        val statusFlow: MutableStateFlow<Map<String, FeedStatus>> = MutableStateFlow(emptyMap()),
    ) {
        var outcome: RefreshOutcome = RefreshOutcome.SUCCESS
        val refreshCallTimes = mutableListOf<Long>()
        val refreshCallFeeds = mutableListOf<List<FeedConfig>>()
    }

    private fun TestScope.mockRepository(repo: Repo = Repo()): RssRepository = mockk {
        every { headlines() } returns repo.headlinesFlow
        every { status() } returns repo.statusFlow
        coEvery { refresh(any()) } coAnswers {
            repo.refreshCallTimes += testScheduler.currentTime
            repo.refreshCallFeeds += firstArg<List<FeedConfig>>()
            repo.outcome
        }
    }

    @Test
    fun `start triggers an immediate refresh when there is no prior success`() = runViewModelTest {
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), Clock.systemUTC())

        viewModel.start()
        runCurrent()

        assertEquals(1, repo.refreshCallTimes.size)
    }

    @Test
    fun `start does not refresh again when the last success is newer than 5 minutes`() = runViewModelTest {
        val repo = Repo()
        var now = Instant.parse("2026-09-10T12:00:00Z")
        val clock = object : Clock() {
            override fun instant() = now
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
        }
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), clock)
        viewModel.refreshNow()
        advanceUntilIdle()
        assertEquals(1, repo.refreshCallTimes.size)

        viewModel.start()
        runCurrent()

        assertEquals(1, repo.refreshCallTimes.size)
    }

    @Test
    fun `start refreshes immediately when the last success is older than 5 minutes`() = runViewModelTest {
        val repo = Repo()
        var now = Instant.parse("2026-09-10T12:00:00Z")
        val clock = object : Clock() {
            override fun instant() = now
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
        }
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), clock)
        viewModel.refreshNow()
        advanceUntilIdle()
        assertEquals(1, repo.refreshCallTimes.size)

        now = now.plusSeconds(6 * 60)
        viewModel.start()
        runCurrent()

        assertEquals(2, repo.refreshCallTimes.size)
    }

    @Test
    fun `refreshes every 30 minutes while started, and stop cancels further refreshes`() = runViewModelTest {
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), Clock.systemUTC())

        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)

        advanceTimeBy(30 * 60_000L + 1)
        runCurrent()
        assertEquals(2, repo.refreshCallTimes.size)

        advanceTimeBy(30 * 60_000L + 1)
        runCurrent()
        assertEquals(3, repo.refreshCallTimes.size)

        viewModel.stop()
        advanceTimeBy(2 * 60 * 60_000L)
        advanceUntilIdle()
        assertEquals(3, repo.refreshCallTimes.size)
    }

    @Test
    fun `failures retry with backoff of 1, 2, 4, 8, 15, 15 minutes, and success resets it`() = runViewModelTest {
        val repo = Repo()
        repo.outcome = RefreshOutcome.ALL_FAILED
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), Clock.systemUTC())

        viewModel.start()
        runCurrent()
        assertEquals(1, repo.refreshCallTimes.size)

        val expectedBackoffMinutes = listOf(1L, 2L, 4L, 8L, 15L, 15L)
        for (minutes in expectedBackoffMinutes) {
            advanceTimeBy(minutes * 60_000L + 1)
            runCurrent()
        }
        assertEquals(1 + expectedBackoffMinutes.size, repo.refreshCallTimes.size)

        repo.outcome = RefreshOutcome.SUCCESS
        advanceTimeBy(15 * 60_000L + 1)
        runCurrent()
        val successCallIndex = repo.refreshCallTimes.size - 1

        advanceTimeBy(30 * 60_000L + 1)
        runCurrent()

        assertEquals(successCallIndex + 2, repo.refreshCallTimes.size)
    }

    @Test
    fun `refreshNow refreshes immediately regardless of the schedule`() = runViewModelTest {
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), Clock.systemUTC())

        viewModel.refreshNow()
        advanceUntilIdle()

        assertEquals(1, repo.refreshCallTimes.size)
        assertEquals(listOf(feedA), repo.refreshCallFeeds.single())
    }

    @Test
    fun `no feeds yields Empty with noFeeds true`() = runViewModelTest {
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), emptyList(), Clock.systemUTC())

        assertEquals(RssUiState.Empty(noFeeds = true), viewModel.uiState.value)

        viewModel.start()
        advanceUntilIdle()
        assertEquals(RssUiState.Empty(noFeeds = true), viewModel.uiState.value)
        assertEquals(0, repo.refreshCallTimes.size)
    }

    @Test
    fun `feeds configured but no headlines yet yields Empty with noFeeds false`() = runViewModelTest {
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), Clock.systemUTC())

        viewModel.start()
        runCurrent()

        assertEquals(RssUiState.Empty(noFeeds = false), viewModel.uiState.value)
    }

    @Test
    fun `headlines with every feed's latest attempt failed are shown as stale`() = runViewModelTest {
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA, feedB), Clock.systemUTC())
        viewModel.start()
        runCurrent()

        repo.headlinesFlow.value = listOf(headline("story"))
        repo.statusFlow.value = mapOf(
            feedA.url to FeedStatus(lastSuccess = null, lastError = "boom"),
            feedB.url to FeedStatus(lastSuccess = null, lastError = "boom"),
        )
        runCurrent()

        val state = viewModel.uiState.value as RssUiState.Showing
        assertTrue(state.stale)
    }

    @Test
    fun `headlines with a recent success and no error are not stale`() = runViewModelTest {
        val fixedNow = Instant.parse("2026-09-10T12:00:00Z")
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val repo = Repo()
        val viewModel = RssViewModel(mockRepository(repo), listOf(feedA), clock)
        viewModel.start()
        runCurrent()

        repo.headlinesFlow.value = listOf(headline("story"))
        repo.statusFlow.value = mapOf(feedA.url to FeedStatus(lastSuccess = fixedNow.minusSeconds(60), lastError = null))
        runCurrent()

        val state = viewModel.uiState.value as RssUiState.Showing
        assertTrue(!state.stale)
    }
}
