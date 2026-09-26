package org.akinosoft.akinoclock.weather.data

import org.akinosoft.akinoclock.util.net.FetchResult
import org.akinosoft.akinoclock.util.net.HttpFetcher
import org.akinosoft.akinoclock.weather.parse.GeocodingJson
import org.akinosoft.akinoclock.weather.parse.WeatherParseException

class OpenMeteoGeocodingClient(private val fetcher: HttpFetcher) : GeocodingClient {

    override suspend fun search(query: String): GeocodingResult {
        val url = OpenMeteoUrls.geocoding(query)
        return when (val result = fetcher.fetch(url, validators = null)) {
            is FetchResult.Success -> try {
                val locations = GeocodingJson.parse(String(result.bytes))
                if (locations.isEmpty()) GeocodingResult.NoResults else GeocodingResult.Found(locations)
            } catch (e: WeatherParseException) {
                GeocodingResult.Failed
            }
            FetchResult.NotModified -> GeocodingResult.Failed
            is FetchResult.Failure -> GeocodingResult.Failed
        }
    }
}
