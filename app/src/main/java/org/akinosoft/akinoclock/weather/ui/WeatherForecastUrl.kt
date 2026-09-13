package org.akinosoft.akinoclock.weather.ui

import java.net.URLEncoder
import org.akinosoft.akinoclock.weather.model.WeatherLocation

/** Open-Meteo (this app's weather API) has no web page of its own, so the strip links out to a
 * public forecast site instead. Returns a plain `String`, not a `Uri` — `android.net.Uri` isn't
 * usable outside Robolectric, and this builder has its own plain-JUnit test. */
object WeatherForecastUrl {

    fun build(location: WeatherLocation): String {
        val name = URLEncoder.encode(location.name, "UTF-8")
        return "https://noadsweather.com/?lat=${location.latitude}&lon=${location.longitude}&name=$name"
    }
}
