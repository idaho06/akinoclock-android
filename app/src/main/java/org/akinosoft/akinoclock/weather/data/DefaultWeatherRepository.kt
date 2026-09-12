package org.akinosoft.akinoclock.weather.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akinosoft.akinoclock.util.FeedCache
import org.akinosoft.akinoclock.util.net.FetchResult
import org.akinosoft.akinoclock.util.net.HttpFetcher
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.akinosoft.akinoclock.weather.model.WeatherReport
import org.akinosoft.akinoclock.weather.parse.WeatherJson
import org.akinosoft.akinoclock.weather.parse.WeatherParseException

/**
 * Combines [HttpFetcher], [WeatherJson] and the (generic, reused) [FeedCache], keyed by the
 * forecast URL so a location change is automatically a cache miss. [primeFromCache] must be called
 * explicitly (typically by the ViewModel) rather than reading in the constructor, so a slow disk
 * read never blocks the thread that constructs this repository. No `If-Modified-Since`: Open-Meteo
 * doesn't honor it, and one small GET per hour is fine.
 */
class DefaultWeatherRepository(
    private val fetcher: HttpFetcher,
    private val cache: FeedCache,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {

    private val _report = MutableStateFlow<WeatherReport?>(null)
    private val mutationDispatcher = ioDispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + mutationDispatcher)
    private var primedUrl: String? = null
    private var lastUrl: String? = null

    override fun report(): Flow<WeatherReport?> = _report.asStateFlow()

    override fun primeFromCache(location: WeatherLocation) {
        val url = forecastUrl(location)
        if (url == primedUrl) return
        primedUrl = url
        lastUrl = url
        scope.launch {
            val cached = cache.read(url) ?: return@launch
            parseOrNull(cached.bytes, Instant.ofEpochMilli(cached.fetchedAtMillis))?.let { _report.value = it }
        }
    }

    override suspend fun refresh(location: WeatherLocation): WeatherRefreshOutcome = withContext(mutationDispatcher) {
        val url = forecastUrl(location)
        when (val result = fetcher.fetch(url, ifModifiedSinceMillis = null)) {
            is FetchResult.Success -> {
                val report = parseOrNull(result.bytes, Instant.now(clock))
                if (report == null) {
                    WeatherRefreshOutcome.FAILED
                } else {
                    cache.write(url, result.bytes)
                    lastUrl?.takeIf { it != url }?.let { cache.clear(it) }
                    lastUrl = url
                    _report.value = report
                    WeatherRefreshOutcome.SUCCESS
                }
            }
            FetchResult.NotModified -> WeatherRefreshOutcome.SUCCESS
            is FetchResult.Failure -> WeatherRefreshOutcome.FAILED
        }
    }

    private fun forecastUrl(location: WeatherLocation): String =
        OpenMeteoUrls.forecast(location.latitude, location.longitude, ZoneId.systemDefault().id)

    private fun parseOrNull(bytes: ByteArray, fetchedAt: Instant): WeatherReport? = try {
        WeatherJson.parse(String(bytes), fetchedAt)
    } catch (e: WeatherParseException) {
        null
    }
}
