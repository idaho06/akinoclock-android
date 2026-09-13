package org.akinosoft.akinoclock.weather.ui

import java.net.URLEncoder
import org.akinosoft.akinoclock.weather.model.WeatherLocation

/** Open-Meteo (this app's weather API) has no web page of its own, so the strip links out to a
 * public forecast site instead. Returns a plain `String`, not a `Uri` — `android.net.Uri` isn't
 * usable outside Robolectric, and this builder has its own plain-JUnit test.
 *
 * `country` is passed through (when known) because noadsweather.com only auto-detects metric
 * units and 24h time from it; with no country param it falls back to guessing from the browser's
 * locale, which picked Fahrenheit/12h on this tablet's Chrome regardless of the actual location. */
object WeatherForecastUrl {

    fun build(location: WeatherLocation): String {
        val name = URLEncoder.encode(location.name, "UTF-8")
        val country = location.country?.let { "&country=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
        return "https://noadsweather.com/?lat=${location.latitude}&lon=${location.longitude}&name=$name$country"
    }
}
