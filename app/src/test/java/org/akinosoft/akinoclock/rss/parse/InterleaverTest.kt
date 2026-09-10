package org.akinosoft.akinoclock.rss.parse

import org.akinosoft.akinoclock.rss.model.Headline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterleaverTest {

    private fun headline(title: String) = Headline(feedTitle = "feed", title = title, link = null, published = null)

    @Test
    fun `round-robins across feeds in order, shorter feeds dropping out`() {
        val a = listOf("a1", "a2", "a3").map(::headline)
        val b = listOf("b1").map(::headline)
        val c = listOf("c1", "c2").map(::headline)

        val result = Interleaver.interleave(listOf(a, b, c))

        assertEquals(listOf("a1", "b1", "c1", "a2", "c2", "a3"), result.map { it.title })
    }

    @Test
    fun `empty inner lists are skipped`() {
        val a = listOf("a1", "a2").map(::headline)
        val empty = emptyList<Headline>()
        val b = listOf("b1").map(::headline)

        val result = Interleaver.interleave(listOf(a, empty, b))

        assertEquals(listOf("a1", "b1", "a2"), result.map { it.title })
    }

    @Test
    fun `all empty feeds yield an empty result`() {
        val result = Interleaver.interleave(listOf(emptyList(), emptyList()))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `no feeds yield an empty result`() {
        assertTrue(Interleaver.interleave(emptyList()).isEmpty())
    }
}
