package org.akinosoft.akinoclock.settings.ui

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedListAdapterTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a feed with a title shows the title and the URL`() {
        val adapter = FeedListAdapter(context()) {}
        adapter.add(FeedConfig(url = "https://example.com/a.xml", title = "A Feed"))

        val view = adapter.getView(0, null, FrameLayout(context()))

        assertEquals("A Feed", view.findViewById<TextView>(R.id.feedTitle).text)
        assertEquals("https://example.com/a.xml", view.findViewById<TextView>(R.id.feedUrl).text)
        assertEquals(View.VISIBLE, view.findViewById<TextView>(R.id.feedUrl).visibility)
    }

    @Test
    fun `a feed with no title shows the URL as the title and hides the URL line`() {
        val adapter = FeedListAdapter(context()) {}
        adapter.add(FeedConfig(url = "https://example.com/b.xml"))

        val view = adapter.getView(0, null, FrameLayout(context()))

        assertEquals("https://example.com/b.xml", view.findViewById<TextView>(R.id.feedTitle).text)
        assertEquals(View.GONE, view.findViewById<TextView>(R.id.feedUrl).visibility)
    }

    @Test
    fun `tapping delete invokes the callback with that feed`() {
        var deleted: FeedConfig? = null
        val adapter = FeedListAdapter(context()) { deleted = it }
        val feed = FeedConfig(url = "https://example.com/a.xml")
        adapter.add(feed)

        val view = adapter.getView(0, null, FrameLayout(context()))
        view.findViewById<View>(R.id.deleteFeedButton).performClick()

        assertEquals(feed, deleted)
    }

    @Test
    fun `re-using a convertView updates it for the new position`() {
        val adapter = FeedListAdapter(context()) {}
        adapter.add(FeedConfig(url = "https://example.com/a.xml", title = "A"))
        adapter.add(FeedConfig(url = "https://example.com/b.xml", title = "B"))

        val firstView = adapter.getView(0, null, FrameLayout(context()))
        val reused = adapter.getView(1, firstView, FrameLayout(context()))

        assertEquals("B", reused.findViewById<TextView>(R.id.feedTitle).text)
    }
}
