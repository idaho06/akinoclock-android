package org.akinosoft.akinoclock.weather.data

import io.mockk.coEvery
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.akinosoft.akinoclock.util.FeedCache
import org.akinosoft.akinoclock.util.net.FetchResult
import org.akinosoft.akinoclock.util.net.HttpFetcher
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWeatherRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fixedClock = Clock.fixed(Instant.parse("2026-09-12T12:00:00Z"), ZoneOffset.UTC)
    private val madrid = WeatherLocation("Madrid, Spain", 40.4168, -3.7038)

    private fun cache() = FeedCache(tempFolder.newFolder("weather-cache"))

    private fun urlFor(location: WeatherLocation) =
        OpenMeteoUrls.forecast(location.latitude, location.longitude, ZoneId.systemDefault().id)

    private val forecastJson = """
        {"current":{"temperature_2m":20.1,"weather_code":0,"is_day":0},
         "daily":{"time":["2026-09-12","2026-09-13","2026-09-14"],"weather_code":[1,1,1],
                  "temperature_2m_max":[31.5,32.1,33.8],"temperature_2m_min":[15.9,17.1,17.4]}}
    """.trimIndent().toByteArray()

    @Test
    fun `cached report for the location's url is emitted before any fetch, read off the caller's thread`() = runTest {
        val cache = cache()
        cache.write(urlFor(madrid), forecastJson)
        val fetcher = mockk<HttpFetcher>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultWeatherRepository(fetcher, cache, fixedClock, dispatcher)

        repository.primeFromCache(madrid)

        // Not loaded yet immediately after priming: the read is dispatched, not run inline.
        assertNull(repository.report().first())

        advanceUntilIdle()

        val report = repository.report().first()
        assertNotNull(report)
        assertEquals(20.1, report!!.current.temperatureC, 0.0)
    }

    @Test
    fun `priming a second location after the first loads that location's cached report`() = runTest {
        val cache = cache()
        val paris = WeatherLocation("Paris, France", 48.8566, 2.3522)
        val parisJson = """
            {"current":{"temperature_2m":9.4,"weather_code":0,"is_day":0},
             "daily":{"time":["2026-09-12","2026-09-13","2026-09-14"],"weather_code":[1,1,1],
                      "temperature_2m_max":[12.5,13.1,13.8],"temperature_2m_min":[5.9,7.1,7.4]}}
        """.trimIndent().toByteArray()
        cache.write(urlFor(madrid), forecastJson)
        cache.write(urlFor(paris), parisJson)
        val fetcher = mockk<HttpFetcher>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultWeatherRepository(fetcher, cache, fixedClock, dispatcher)

        repository.primeFromCache(madrid)
        advanceUntilIdle()
        assertEquals(20.1, repository.report().first()!!.current.temperatureC, 0.0)

        repository.primeFromCache(paris)
        advanceUntilIdle()

        assertEquals(9.4, repository.report().first()!!.current.temperatureC, 0.0)
    }

    @Test
    fun `refresh success writes cache and emits the report`() = runTest {
        val cache = cache()
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success(forecastJson, null)
        val repository = DefaultWeatherRepository(fetcher, cache, fixedClock)

        val outcome = repository.refresh(madrid)

        assertEquals(WeatherRefreshOutcome.SUCCESS, outcome)
        val report = repository.report().first()
        assertNotNull(report)
        assertEquals(Instant.now(fixedClock), report!!.fetchedAt)
        assertNotNull(cache.read(urlFor(madrid)))
    }

    @Test
    fun `fetch failure keeps the previous report`() = runTest {
        val cache = cache()
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success(forecastJson, null) andThen
            FetchResult.Failure(FetchResult.Failure.Reason.Http(500))
        val repository = DefaultWeatherRepository(fetcher, cache, fixedClock)

        repository.refresh(madrid)
        val previous = repository.report().first()
        val outcome = repository.refresh(madrid)

        assertEquals(WeatherRefreshOutcome.FAILED, outcome)
        assertEquals(previous, repository.report().first())
    }

    @Test
    fun `unparsable success body fails and leaves the cache untouched`() = runTest {
        val cache = cache()
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success(forecastJson, null) andThen
            FetchResult.Success("not json".toByteArray(), null)
        val repository = DefaultWeatherRepository(fetcher, cache, fixedClock)

        repository.refresh(madrid)
        val previousReport = repository.report().first()
        val previousCache = cache.read(urlFor(madrid))

        val outcome = repository.refresh(madrid)

        assertEquals(WeatherRefreshOutcome.FAILED, outcome)
        assertEquals(previousReport, repository.report().first())
        assertEquals(previousCache?.bytes?.toList(), cache.read(urlFor(madrid))?.bytes?.toList())
    }

    @Test
    fun `refreshing a new location writes a new file and deletes the old one`() = runTest {
        val cache = cache()
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success(forecastJson, null)
        val repository = DefaultWeatherRepository(fetcher, cache, fixedClock)

        repository.refresh(madrid)
        assertNotNull(cache.read(urlFor(madrid)))

        val paris = WeatherLocation("Paris, France", 48.8566, 2.3522)
        repository.refresh(paris)

        assertNotNull(cache.read(urlFor(paris)))
        assertNull(cache.read(urlFor(madrid)))
    }
}
