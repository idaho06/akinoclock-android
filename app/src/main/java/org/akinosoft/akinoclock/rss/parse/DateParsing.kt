package org.akinosoft.akinoclock.rss.parse

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Tolerant parsing of RSS `pubDate` (RFC-1123) and Atom `updated`/`published` (ISO-8601). */
object DateParsing {

    fun parse(text: String): Instant? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        return runCatching { OffsetDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
            .recoverCatching { OffsetDateTime.parse(trimmed).toInstant() }
            .getOrNull()
    }
}
