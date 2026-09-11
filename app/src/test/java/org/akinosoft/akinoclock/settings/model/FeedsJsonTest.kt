package org.akinosoft.akinoclock.settings.model

import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedsJsonTest {

    @Test
    fun `round-trips a list with and without titles`() {
        val feeds = listOf(
            FeedConfig(url = "https://example.com/a.xml", title = "A Feed"),
            FeedConfig(url = "https://example.com/b.xml"),
        )

        val decoded = FeedsJson.decode(FeedsJson.encode(feeds))

        assertEquals(feeds, decoded)
    }

    @Test
    fun `empty list round-trips to an empty JSON array`() {
        assertEquals("[]", FeedsJson.encode(emptyList()))
        assertTrue(FeedsJson.decode("[]").isEmpty())
    }

    @Test
    fun `malformed JSON decodes to an empty list instead of crashing`() {
        assertTrue(FeedsJson.decode("not json").isEmpty())
        assertTrue(FeedsJson.decode("{\"oops\": true}").isEmpty())
        assertTrue(FeedsJson.decode("[{\"title\": \"no url\"}]").isEmpty())
    }

    @Test
    fun `unknown keys in a feed object are ignored`() {
        val decoded = FeedsJson.decode(
            """[{"url": "https://example.com/a.xml", "title": "A", "extra": "ignored"}]""",
        )

        assertEquals(listOf(FeedConfig(url = "https://example.com/a.xml", title = "A")), decoded)
    }
}
