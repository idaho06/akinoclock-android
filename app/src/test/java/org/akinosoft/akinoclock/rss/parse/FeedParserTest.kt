package org.akinosoft.akinoclock.rss.parse

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedParserTest {

    private fun fixture(name: String) =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("rss/$name")) { "missing fixture $name" }

    @Test
    fun `parses RSS 2 0 basic feed`() {
        val parsed = FeedParser.parse(fixture("rss2-basic.xml"))

        assertEquals("Example Feed", parsed.title)
        assertEquals(3, parsed.items.size)
        val first = parsed.items.first()
        assertEquals("Third item", first.title)
        assertEquals("https://example.com/3", first.link)
        assertEquals(Instant.parse("2026-09-10T10:00:00Z"), first.published)
    }

    @Test
    fun `strips HTML and decodes entities and CDATA titles`() {
        val parsed = FeedParser.parse(fixture("rss2-cdata-html.xml"))

        assertEquals(1, parsed.items.size)
        assertEquals("Bold & Italic title", parsed.items.single().title)
    }

    @Test
    fun `atom prefers alternate link over self and tolerates missing link`() {
        val parsed = FeedParser.parse(fixture("atom-basic.xml"))

        assertEquals("Example Atom Feed", parsed.title)
        assertEquals(3, parsed.items.size)

        val withAlternate = parsed.items.first { it.title == "Atom entry with alternate and self links" }
        assertEquals("https://example.com/atom/1", withAlternate.link)
        assertEquals(Instant.parse("2026-09-10T08:00:00Z"), withAlternate.published)

        val withPlainLink = parsed.items.first { it.title == "Atom entry with no rel attribute" }
        assertEquals("https://example.com/atom/2", withPlainLink.link)
        assertEquals(Instant.parse("2026-09-10T09:00:00Z"), withPlainLink.published)

        val withoutLink = parsed.items.first { it.title == "Atom entry with no link" }
        assertNull(withoutLink.link)
    }

    @Test
    fun `finds items at the RDF RSS1 0 root level`() {
        val parsed = FeedParser.parse(fixture("rdf-rss1.xml"))

        assertEquals("RDF Feed", parsed.title)
        assertEquals(2, parsed.items.size)
        assertEquals(setOf("RDF item one", "RDF item two"), parsed.items.map { it.title }.toSet())
    }

    @Test
    fun `malformed XML throws FeedParseException`() {
        assertThrows(FeedParseException::class.java) { FeedParser.parse(fixture("malformed.xml")) }
    }

    @Test
    fun `empty channel yields empty list without exception`() {
        val parsed = FeedParser.parse(fixture("empty-channel.xml"))

        assertEquals("Empty Feed", parsed.title)
        assertTrue(parsed.items.isEmpty())
    }

    @Test
    fun `caps at 20 newest items sorted by published descending`() {
        val parsed = FeedParser.parse(fixture("huge-titles.xml"))

        assertEquals(20, parsed.items.size)
        assertEquals("Item 25", parsed.items.first().title)
        assertEquals("Item 6", parsed.items.last().title)
        val published = parsed.items.map { requireNotNull(it.published) }
        assertEquals(published.sortedDescending(), published)
    }

    @Test
    fun `parses a real-world feed sample without crashing`() {
        val parsed = FeedParser.parse(fixture("bbc-world-sample.xml"))

        assertEquals("BBC News", parsed.title)
        assertEquals(20, parsed.items.size)
        assertTrue(parsed.items.all { it.title.isNotBlank() })
    }
}
