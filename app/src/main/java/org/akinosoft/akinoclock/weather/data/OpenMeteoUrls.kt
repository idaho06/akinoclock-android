package org.akinosoft.akinoclock.weather.data

import java.net.URLEncoder
import java.util.Locale

/** Builds Open-Meteo URLs. Pure and tested for the exact string — a mismatch here means a 400 from
 * the API, and `Locale.ROOT` for number formatting is mandatory: a Spanish device locale would
 * otherwise emit "40,4168" in place of "40.4168". */
object OpenMeteoUrls {

    fun forecast(latitude: Double, longitude: Double, timeZoneId: String): String {
        val lat = String.format(Locale.ROOT, "%.4f", latitude)
        val lon = String.format(Locale.ROOT, "%.4f", longitude)
        val tz = URLEncoder.encode(timeZoneId, "UTF-8")
        return "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=$tz&forecast_days=3"
    }

    fun geocoding(query: String): String {
        val name = URLEncoder.encode(query.trim(), "UTF-8")
        return "https://geocoding-api.open-meteo.com/v1/search?name=$name&count=5&language=en&format=json"
    }
}
