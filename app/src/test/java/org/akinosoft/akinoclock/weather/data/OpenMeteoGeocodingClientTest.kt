package org.akinosoft.akinoclock.weather.data

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.akinosoft.akinoclock.util.net.FetchResult
import org.akinosoft.akinoclock.util.net.HttpFetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoGeocodingClientTest {

    private fun fixture(name: String) =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("weather/$name")) { "missing fixture $name" }
            .readBytes()

    @Test
    fun `found results are parsed`() = runTest {
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success(fixture("geocoding-madrid.json"), null)
        val client = OpenMeteoGeocodingClient(fetcher)

        val result = client.search("Madrid")

        assertTrue(result is GeocodingResult.Found)
        assertEquals(2, (result as GeocodingResult.Found).locations.size)
    }

    @Test
    fun `no results`() = runTest {
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success(fixture("geocoding-no-results.json"), null)
        val client = OpenMeteoGeocodingClient(fetcher)

        assertEquals(GeocodingResult.NoResults, client.search("Nonsenseville"))
    }

    @Test
    fun `http failure`() = runTest {
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Failure(FetchResult.Failure.Reason.Http(500))
        val client = OpenMeteoGeocodingClient(fetcher)

        assertEquals(GeocodingResult.Failed, client.search("Madrid"))
    }

    @Test
    fun `io failure`() = runTest {
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Failure(FetchResult.Failure.Reason.Io("boom"))
        val client = OpenMeteoGeocodingClient(fetcher)

        assertEquals(GeocodingResult.Failed, client.search("Madrid"))
    }

    @Test
    fun `malformed body is a failure`() = runTest {
        val fetcher = mockk<HttpFetcher>()
        coEvery { fetcher.fetch(any(), null) } returns FetchResult.Success("not json".toByteArray(), null)
        val client = OpenMeteoGeocodingClient(fetcher)

        assertEquals(GeocodingResult.Failed, client.search("Madrid"))
    }
}
