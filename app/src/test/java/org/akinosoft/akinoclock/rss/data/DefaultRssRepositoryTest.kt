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
import org.akinosoft.akinoclock.util.FeedCache
import org.akinosoft.akinoclock.util.net.CacheValidators
import org.akinosoft.akinoclock.util.net.FetchResult
import org.akinosoft.akinoclock.util.net.HttpFetcher
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
            val fetcher = mockk<HttpFetcher>()

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
        val fetcher = mockk<HttpFetcher>()
        val bytes = rss("Fresh story")
        coEvery { fetcher.fetch(feed.url, null) } returns FetchResult.Success(bytes, validators = null)
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
    fun `refresh success stores the returned validators for the next conditional fetch`() = runTest {
        val feed = FeedConfig(url = "https://example.com/feed.xml")
        val cache = cache()
        val validators = CacheValidators(etag = "\"v2\"", lastModified = null)
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(feed.url, null) } returns FetchResult.Success(rss("Fresh story"), validators)
        val repository = DefaultRssRepository(listOf(feed), fetcher, cache, fixedClock)

        repository.refresh(listOf(feed))

        assertEquals(validators, cache.readValidators(feed.url))
    }

    @Test
    fun `a cached feed without stored validators is fetched unconditionally`() = runTest {
        val feed = FeedConfig(url = "https://example.com/feed.xml")
        val cache = cache()
        cache.write(feed.url, rss("Cached story"))
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(feed.url, null) } returns FetchResult.NotModified
        val repository = DefaultRssRepository(listOf(feed), fetcher, cache, fixedClock)

        repository.refresh(listOf(feed))

        coVerify(exactly = 1) { fetcher.fetch(feed.url, null) }
    }

    @Test
    fun `refresh failure for one of three feeds keeps its cached headlines`() = runTest {
        val a = FeedConfig(url = "https://example.com/a.xml")
        val b = FeedConfig(url = "https://example.com/b.xml")
        val c = FeedConfig(url = "https://example.com/c.xml")
        val cache = cache()
        cache.write(b.url, rss("Old b story"))
        val fetcher = mockk<HttpFetcher>()
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
    fun `stored validators are sent, and a 304 touches cache mtime, leaves headlines unchanged, counts as success`() = runTest {
        val feed = FeedConfig(url = "https://example.com/feed.xml")
        var currentMillis = 1_000L
        val cache = FeedCache(tempFolder.newFolder("rss-cache")) { currentMillis }
        val validators = CacheValidators(etag = "\"v1\"", lastModified = "Sat, 26 Sep 2026 10:00:00 GMT")
        cache.write(feed.url, rss("Unchanged story"), validators)
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(feed.url, validators) } returns FetchResult.NotModified
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
        val fetcher = mockk<HttpFetcher>()
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
        val fetcher = mockk<HttpFetcher>()
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
