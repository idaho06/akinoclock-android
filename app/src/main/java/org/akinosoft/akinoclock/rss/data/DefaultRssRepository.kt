package org.akinosoft.akinoclock.rss.data

import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.rss.model.FeedStatus
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.rss.parse.FeedParseException
import org.akinosoft.akinoclock.rss.parse.FeedParser
import org.akinosoft.akinoclock.rss.parse.Interleaver
import org.akinosoft.akinoclock.util.FeedCache
import org.akinosoft.akinoclock.util.net.FetchResult
import org.akinosoft.akinoclock.util.net.HttpFetcher

/**
 * Combines [HttpFetcher], [FeedParser] and [FeedCache]: cached headlines are loaded off the
 * constructing thread and become available once that finishes (before any network call), and
 * [refresh] fetches feeds sequentially (this device has no need for parallel connections)
 * updating the cache, the headlines/status flows, and pruning feeds no longer in the configured
 * list. Reading and parsing every cached feed's XML is real CPU/IO work — on a slow device with a
 * warm cache this can take seconds, so it must never run on the thread that constructs this
 * repository (typically the app's main thread via `AppContainer`).
 *
 * [mutationDispatcher] confines every read-modify-write of [headlinesByUrl]/[statusByUrl]/
 * [trackedFeeds] (both the background cache warm-up below and [refresh]) to a single logical
 * thread, so the two can never race on that shared state.
 */
class DefaultRssRepository(
    initialFeeds: List<FeedConfig>,
    private val fetcher: HttpFetcher,
    private val cache: FeedCache,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RssRepository {

    private var trackedFeeds: List<FeedConfig> = emptyList()
    private val headlinesByUrl = mutableMapOf<String, List<Headline>>()
    private val statusByUrl = mutableMapOf<String, FeedStatus>()

    private val _headlines = MutableStateFlow<List<Headline>>(emptyList())
    private val _status = MutableStateFlow<Map<String, FeedStatus>>(emptyMap())

    private val mutationDispatcher = ioDispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + mutationDispatcher)

    init {
        trackedFeeds = initialFeeds
        scope.launch {
            initialFeeds.forEach { loadFromCache(it) }
            recompute(initialFeeds)
        }
    }

    override fun headlines(): Flow<List<Headline>> = _headlines.asStateFlow()

    override fun status(): Flow<Map<String, FeedStatus>> = _status.asStateFlow()

    override suspend fun refresh(feeds: List<FeedConfig>): RefreshOutcome = withContext(mutationDispatcher) {
        dropRemovedFeeds(feeds)
        feeds.filter { it.url !in headlinesByUrl }.forEach { loadFromCache(it) }
        trackedFeeds = feeds

        var successCount = 0
        for (feed in feeds) {
            if (fetchOne(feed)) successCount++
        }
        recompute(feeds)

        when {
            feeds.isEmpty() -> RefreshOutcome.NO_FEEDS
            successCount == feeds.size -> RefreshOutcome.SUCCESS
            successCount == 0 -> RefreshOutcome.ALL_FAILED
            else -> RefreshOutcome.PARTIAL_FAILURE
        }
    }

    /** Fetches one feed and applies the result to the cache/headlines/status maps; returns success. */
    private suspend fun fetchOne(feed: FeedConfig): Boolean {
        val ifModifiedSince = cache.read(feed.url)?.fetchedAtMillis
        return when (val result = withContext(ioDispatcher) { fetcher.fetch(feed.url, ifModifiedSince) }) {
            is FetchResult.Success -> {
                cache.write(feed.url, result.bytes)
                headlinesByUrl[feed.url] = parseHeadlines(feed, result.bytes)
                markSuccess(feed.url)
                true
            }
            FetchResult.NotModified -> {
                cache.touch(feed.url)
                markSuccess(feed.url)
                true
            }
            is FetchResult.Failure -> {
                val previousSuccess = statusByUrl[feed.url]?.lastSuccess
                statusByUrl[feed.url] = FeedStatus(lastSuccess = previousSuccess, lastError = describe(result.reason))
                false
            }
        }
    }

    private fun markSuccess(url: String) {
        statusByUrl[url] = FeedStatus(lastSuccess = Instant.now(clock), lastError = null)
    }

    private fun dropRemovedFeeds(newFeeds: List<FeedConfig>) {
        val newUrls = newFeeds.map { it.url }.toSet()
        trackedFeeds.filter { it.url !in newUrls }.forEach { removed ->
            cache.clear(removed.url)
            headlinesByUrl.remove(removed.url)
            statusByUrl.remove(removed.url)
        }
    }

    private fun loadFromCache(feed: FeedConfig) {
        val cached = cache.read(feed.url) ?: return
        headlinesByUrl[feed.url] = parseHeadlines(feed, cached.bytes)
    }

    private fun parseHeadlines(feed: FeedConfig, bytes: ByteArray): List<Headline> {
        val parsed = try {
            FeedParser.parse(bytes.inputStream())
        } catch (e: FeedParseException) {
            return headlinesByUrl[feed.url].orEmpty()
        }
        val feedTitle = feed.title ?: parsed.title ?: feed.url
        return parsed.items.map { Headline(feedTitle, it.title, it.link, it.published) }
    }

    private fun recompute(feeds: List<FeedConfig>) {
        _headlines.value = Interleaver.interleave(feeds.map { headlinesByUrl[it.url].orEmpty() })
        _status.value = statusByUrl.toMap()
    }

    private fun describe(reason: FetchResult.Failure.Reason): String = when (reason) {
        is FetchResult.Failure.Reason.Http -> "HTTP ${reason.code}"
        is FetchResult.Failure.Reason.Io -> reason.message ?: "network error"
        FetchResult.Failure.Reason.TooLarge -> "response too large"
    }
}
