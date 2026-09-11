package org.akinosoft.akinoclock.settings.model

import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Encodes/decodes the `feeds` preference value. Malformed JSON decodes to an empty list rather
 * than crashing, since it may be read back from a corrupted or manually-edited preference. */
object FeedsJson {

    fun encode(feeds: List<FeedConfig>): String {
        val array = JSONArray()
        feeds.forEach { feed ->
            val obj = JSONObject()
            obj.put("url", feed.url)
            feed.title?.let { obj.put("title", it) }
            array.put(obj)
        }
        return array.toString()
    }

    fun decode(json: String): List<FeedConfig> = try {
        val array = JSONArray(json)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            FeedConfig(
                url = obj.getString("url"),
                title = if (obj.has("title")) obj.getString("title") else null,
            )
        }
    } catch (e: JSONException) {
        emptyList()
    }
}
