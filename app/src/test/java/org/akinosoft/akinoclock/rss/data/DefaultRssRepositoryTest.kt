package org.akinosoft.akinoclock.rss.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRssRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fixedClock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC)

    private fun cache(nowMillis: Long = 1_000L) = FeedCache(tempFolder.newFolder("rss-cache")) { nowMillis }

    private fun rss(vararg titles: String) = """
        <rss version="2.0"><channel><title>Feed</title>
        ${titles.joinToString("") { "<item><title>$it</title><link>https://example.com/$it</link></item>" }}
        </channel></rss>
    """.trimIndent().toByteArray()

    @Test
    fun `headlines eventually reflect cached content, loaded off the caller's thread, without any network call`() =
        runTest {
            val feed = FeedConfig(url = "https://example.com/feed.xml")
            val cache = cache()
            cache.write(feed.url, rss("Cached story"))
            val fetcher = mockk<FeedFetcher>()

            val repository = DefaultRssRepository(
                listOf(feed), fetcher, cache, fixedClock,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

            // The cache load is dispatched, not run inline on the constructing thread: nothing
            // is loaded yet immediately after construction returns.
            assertEquals(emptyList<String>(), repository.headlines().first().map { it.title })

            advanceUntilIdle()

            assertEquals(listOf("Cached story"), repository.headlines().first().map { it.title })
            coVerify(exactly = 0) { fetcher.fetch(any(), any()) }
        }

    @Test
    fun `refresh success updates cache, headlines and status`() = runTest {
        val feed = FeedConfig(url = "https://example.com/feed.xml")
        val cache = cache()
        val fetcher = mockk<FeedFetcher>()
        val bytes = rss("Fresh story")
        coEvery { fetcher.fetch(feed.url, null) } returns FetchResult.Success(bytes, lastModifiedMillis = null)
        val repository = DefaultRssRepository(listOf(feed), fetcher, cache, fixedClock)

        val outcome = repository.refresh(listOf(feed))

        assertEquals(RefreshOutcome.SUCCESS, outcome)
        assertEquals(bytes.toList(), cache.read(feed.url)?.bytes?.toList())
        assertEquals(listOf("Fresh story"), repository.headlines().first().map { it.title })
        val status = repository.status().first().getValue(feed.url)
        assertEquals(Instant.now(fixedClock), status.lastSuccess)
        assertNull(status.lastError)
    }

    @Test
    fun `refresh failure for one of three feeds keeps its cached headlines`() = runTest {
        val a = FeedConfig(url = "https://example.com/a.xml")
        val b = FeedConfig(url = "https://example.com/b.xml")
        val c = FeedConfig(url = "https://example.com/c.xml")
        val cache = cache()
        cache.write(b.url, rss("Old b story"))
        val fetcher = mockk<FeedFetcher>()
        coEvery { fetcher.fetch(a.url, null) } returns FetchResult.Success(rss("New a story"), null)
        coEvery { fetcher.fetch(b.url, any()) } returns FetchResult.Failure(FetchResult.Failure.Reason.Http(500))
        coEvery { fetcher.fetch(c.url, null) } returns FetchResult.Success(rss("New c story"), null)
        val repository = DefaultRssRepository(listOf(a, b, c), fetcher, cache, fixedClock)

        val outcome = repository.refresh(listOf(a, b, c))

        assertEquals(RefreshOutcome.PARTIAL_FAILURE, outcome)
        val titles = repository.headlines().first().map { it.title }.toSet()
        assertTrue(titles.containsAll(setOf("New a story", "Old b story", "New c story")))
        val status = repository.status().first()
        assertEquals("HTTP 500", status.getValue(b.url).lastError)
        assertNull(status.getValue(a.url).lastError)
    }

    @Test
    fun `304 touches cache mtime, leaves headlines unchanged, counts as success`() = runTest {
        val feed = FeedConfig(url = "https://example.com/feed.xml")
        var currentMillis = 1_000L
        val cache = FeedCache(tempFolder.newFolder("rss-cache")) { currentMillis }
        cache.write(feed.url, rss("Unchanged story"))
        val fetcher = mockk<FeedFetcher>()
        coEvery { fetcher.fetch(feed.url, 1_000L) } returns FetchResult.NotModified
        val repository = DefaultRssRepository(listOf(feed), fetcher, cache, fixedClock)

        currentMillis = 5_000L
        val outcome = repository.refresh(listOf(feed))

        assertEquals(RefreshOutcome.SUCCESS, outcome)
        assertEquals(5_000L, cache.read(feed.url)?.fetchedAtMillis)
        assertEquals(listOf("Unchanged story"), repository.headlines().first().map { it.title })
    }

    @Test
    fun `feed removed from config drops its headlines and deletes its cache file`() = runTest {
        val a = FeedConfig(url = "https://example.com/a.xml")
        val b = FeedConfig(url = "https://example.com/b.xml")
        val cache = cache()
        cache.write(a.url, rss("a story"))
        cache.write(b.url, rss("b story"))
        val fetcher = mockk<FeedFetcher>()
        coEvery { fetcher.fetch(a.url, any()) } returns FetchResult.NotModified
        val repository = DefaultRssRepository(listOf(a, b), fetcher, cache, fixedClock)

        repository.refresh(listOf(a))

        assertEquals(listOf("a story"), repository.headlines().first().map { it.title })
        assertNull(cache.read(b.url))
        coVerify(exactly = 0) { fetcher.fetch(b.url, any()) }
    }

    @Test
    fun `fetching is sequential per refresh, in configured order`() = runTest {
        val a = FeedConfig(url = "https://example.com/a.xml")
        val b = FeedConfig(url = "https://example.com/b.xml")
        val c = FeedConfig(url = "https://example.com/c.xml")
        val cache = cache()
        val fetcher = mockk<FeedFetcher>()
        coEvery { fetcher.fetch(any(), any()) } returns FetchResult.NotModified
        val repository = DefaultRssRepository(listOf(a, b, c), fetcher, cache, fixedClock)

        repository.refresh(listOf(a, b, c))

        coVerifyOrder {
            fetcher.fetch(a.url, null)
            fetcher.fetch(b.url, null)
            fetcher.fetch(c.url, null)
        }
    }
}
