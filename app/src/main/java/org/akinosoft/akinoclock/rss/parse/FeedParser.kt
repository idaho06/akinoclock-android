package org.akinosoft.akinoclock.rss.parse

import java.io.IOException
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parses RSS 2.0, Atom 1.0 and RSS 1.0/RDF feeds (all `<item>`/`<entry>` at any depth) into a
 * flat [ParsedFeed], pure over streams so it can be unit-tested without Robolectric.
 */
object FeedParser {

    private const val MAX_ITEMS = 20

    // An Atom entry can carry several <link> elements; rel="alternate" (or no rel, which
    // defaults to "alternate" per the Atom spec) is the one that points at the article.
    private const val LINK_PRIORITY_ALTERNATE = 1
    private const val LINK_PRIORITY_OTHER = 0

    // An Atom entry can carry both <published> and <updated>; <published> is the one that
    // matches RSS's <pubDate> semantics (original publish time) so it wins when both are present.
    private const val DATE_PRIORITY_PUBLISHED = 1
    private const val DATE_PRIORITY_OTHER = 0

    fun parse(input: InputStream): ParsedFeed {
        val parser = try {
            XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        } catch (e: XmlPullParserException) {
            throw FeedParseException("failed to create XML parser", e)
        }

        try {
            parser.setInput(input, null)

            var channelTitle: String? = null
            val items = mutableListOf<ParsedItem>()
            var hasItemWithoutPublished = false

            var inItem = false
            var itemTitle: String? = null
            var itemLink: String? = null
            var itemLinkPriority = -1
            var itemPublished: String? = null
            var itemPublishedPriority = -1

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item", "entry" -> {
                            inItem = true
                            itemTitle = null
                            itemLink = null
                            itemLinkPriority = -1
                            itemPublished = null
                            itemPublishedPriority = -1
                        }
                        "title" -> {
                            val text = parser.nextText()
                            if (inItem) {
                                if (itemTitle == null) itemTitle = text
                            } else if (channelTitle == null) {
                                channelTitle = HtmlText.strip(text)
                            }
                        }
                        "link" -> if (inItem) {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                val rel = parser.getAttributeValue(null, "rel")
                                val priority = if (rel == null || rel == "alternate") LINK_PRIORITY_ALTERNATE else LINK_PRIORITY_OTHER
                                if (priority > itemLinkPriority) {
                                    itemLink = href
                                    itemLinkPriority = priority
                                }
                            } else {
                                itemLink = parser.nextText()
                            }
                        }
                        "pubDate", "updated", "published" -> if (inItem) {
                            val text = parser.nextText()
                            val priority = if (parser.name == "published") DATE_PRIORITY_PUBLISHED else DATE_PRIORITY_OTHER
                            if (priority >= itemPublishedPriority) {
                                itemPublished = text
                                itemPublishedPriority = priority
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && (parser.name == "item" || parser.name == "entry")) {
                    inItem = false
                    val title = itemTitle?.let { HtmlText.strip(it) }.orEmpty()
                    if (title.isNotEmpty()) {
                        val published = itemPublished?.let(DateParsing::parse)
                        if (published == null) hasItemWithoutPublished = true
                        items += ParsedItem(title, itemLink, published)
                    }
                }
                eventType = parser.next()
            }

            val ordered = if (items.isNotEmpty() && !hasItemWithoutPublished) {
                items.sortedByDescending { it.published }
            } else {
                items
            }
            return ParsedFeed(channelTitle, ordered.take(MAX_ITEMS))
        } catch (e: XmlPullParserException) {
            throw FeedParseException("malformed feed XML", e)
        } catch (e: IOException) {
            throw FeedParseException("failed reading feed XML", e)
        }
    }
}
