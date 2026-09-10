package org.akinosoft.akinoclock.rss.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {

    @Test
    fun `strips tags`() {
        assertEquals("Hello world", HtmlText.strip("<b>Hello</b> <i>world</i>"))
    }

    @Test
    fun `decodes common named entities`() {
        assertEquals("Tom & Jerry", HtmlText.strip("Tom &amp; Jerry"))
        assertEquals("<quoted>", HtmlText.strip("&lt;quoted&gt;"))
        assertEquals("\"quote\" 'apos'", HtmlText.strip("&quot;quote&quot; &apos;apos&apos;"))
        assertEquals("non breaking space", HtmlText.strip("non&nbsp;breaking&nbsp;space"))
    }

    @Test
    fun `decodes numeric entities`() {
        assertEquals("A", HtmlText.strip("&#65;"))
        assertEquals("A", HtmlText.strip("&#x41;"))
    }

    @Test
    fun `collapses whitespace`() {
        assertEquals("a b c", HtmlText.strip("a\n\n  b\t\tc  "))
    }

    @Test
    fun `handles plain text unchanged aside from trimming`() {
        assertEquals("Plain title", HtmlText.strip("Plain title"))
    }

    @Test
    fun `handles empty and blank input`() {
        assertEquals("", HtmlText.strip(""))
        assertEquals("", HtmlText.strip("   \n\t  "))
    }
}
