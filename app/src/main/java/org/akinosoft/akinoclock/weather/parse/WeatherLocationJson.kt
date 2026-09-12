package org.akinosoft.akinoclock.weather.parse

import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.json.JSONException
import org.json.JSONObject

/** Encodes/decodes the `weather_location` preference value. Malformed JSON decodes to `null`
 * rather than crashing, since it may be read back from a corrupted or manually-edited preference. */
object WeatherLocationJson {

    fun encode(location: WeatherLocation): String = JSONObject().apply {
        put("name", location.name)
        put("lat", location.latitude)
        put("lon", location.longitude)
    }.toString()

    fun decode(json: String): WeatherLocation? = try {
        val obj = JSONObject(json)
        WeatherLocation(
            name = obj.getString("name"),
            latitude = obj.getDouble("lat"),
            longitude = obj.getDouble("lon"),
        )
    } catch (e: JSONException) {
        null
    }
}
