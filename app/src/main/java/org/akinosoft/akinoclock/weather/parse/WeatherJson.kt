package org.akinosoft.akinoclock.weather.parse

import java.time.Instant
import java.time.LocalDate
import org.akinosoft.akinoclock.weather.model.CurrentWeather
import org.akinosoft.akinoclock.weather.model.DayForecast
import org.akinosoft.akinoclock.weather.model.WeatherCondition
import org.akinosoft.akinoclock.weather.model.WeatherReport
import org.json.JSONException
import org.json.JSONObject

/** Parses an Open-Meteo `/v1/forecast` response body. [days] must have exactly 3 entries — the
 * strip layout assumes today plus a two-day forecast. */
object WeatherJson {

    private const val EXPECTED_DAYS = 3

    fun parse(json: String, fetchedAt: Instant): WeatherReport = try {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxes = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")

        if (dates.length() != EXPECTED_DAYS) {
            throw WeatherParseException("expected $EXPECTED_DAYS forecast days, got ${dates.length()}")
        }

        val days = (0 until EXPECTED_DAYS).map { i ->
            DayForecast(
                date = LocalDate.parse(dates.getString(i)),
                condition = WeatherCondition.fromWmoCode(codes.getInt(i)),
                minC = mins.getDouble(i),
                maxC = maxes.getDouble(i),
            )
        }

        WeatherReport(
            current = CurrentWeather(
                temperatureC = current.getDouble("temperature_2m"),
                condition = WeatherCondition.fromWmoCode(current.getInt("weather_code")),
                isDay = current.getInt("is_day") != 0,
            ),
            days = days,
            fetchedAt = fetchedAt,
        )
    } catch (e: WeatherParseException) {
        throw e
    } catch (e: JSONException) {
        throw WeatherParseException("malformed forecast response", e)
    }
}
