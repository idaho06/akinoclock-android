package org.akinosoft.akinoclock.rss.parse

import java.time.Instant

data class ParsedFeed(val title: String?, val items: List<ParsedItem>)

data class ParsedItem(val title: String, val link: String?, val published: Instant?)

class FeedParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
