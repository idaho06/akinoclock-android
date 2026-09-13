package org.akinosoft.akinoclock.weather.parse

import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.json.JSONException
import org.json.JSONObject

/** Parses an Open-Meteo geocoding-search response body. A query with no matches returns a body
 * without a `results` key at all — that's not an error, just an empty list. */
object GeocodingJson {

    fun parse(json: String): List<WeatherLocation> = try {
        val root = JSONObject(json)
        if (!root.has("results")) {
            emptyList()
        } else {
            val results = root.getJSONArray("results")
            (0 until results.length()).map { i -> toLocation(results.getJSONObject(i)) }
        }
    } catch (e: JSONException) {
        throw WeatherParseException("malformed geocoding response", e)
    }

    private fun toLocation(obj: JSONObject): WeatherLocation {
        val name = obj.getString("name")
        val admin1 = obj.optString("admin1", "").takeIf { it.isNotEmpty() && it != name }
        val country = obj.optString("country", "").takeIf { it.isNotEmpty() }
        val label = listOfNotNull(name, admin1, country).joinToString(", ")
        return WeatherLocation(
            name = label,
            latitude = obj.getDouble("latitude"),
            longitude = obj.getDouble("longitude"),
            country = country,
        )
    }
}
