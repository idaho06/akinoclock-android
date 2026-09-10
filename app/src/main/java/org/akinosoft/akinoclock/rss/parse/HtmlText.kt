package org.akinosoft.akinoclock.rss.parse

/**
 * Strips HTML tags and decodes entities from feed titles without `android.text.Html`, which is
 * a stub on the host JVM and unavailable to plain-JUnit parser tests.
 */
object HtmlText {

    private val TAG_REGEX = Regex("<[^>]*>")
    private val NAMED_ENTITY_REGEX = Regex("&(amp|lt|gt|quot|apos|nbsp);")
    private val DECIMAL_ENTITY_REGEX = Regex("&#(\\d+);")
    private val HEX_ENTITY_REGEX = Regex("&#x([0-9a-fA-F]+);")
    private val WHITESPACE_REGEX = Regex("\\s+")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
    )

    fun strip(input: String): String {
        val withoutTags = TAG_REGEX.replace(input, "")
        val withoutHexEntities = HEX_ENTITY_REGEX.replace(withoutTags) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
        val withoutDecimalEntities = DECIMAL_ENTITY_REGEX.replace(withoutHexEntities) { match ->
            match.groupValues[1].toInt().toChar().toString()
        }
        val withoutNamedEntities = NAMED_ENTITY_REGEX.replace(withoutDecimalEntities) { match ->
            NAMED_ENTITIES.getValue(match.groupValues[1])
        }
        return WHITESPACE_REGEX.replace(withoutNamedEntities, " ").trim()
    }
}
