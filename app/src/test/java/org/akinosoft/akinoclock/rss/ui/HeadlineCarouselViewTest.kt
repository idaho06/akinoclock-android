package org.akinosoft.akinoclock.rss.ui

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.TimeUnit
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.util.FakePeriodicScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class HeadlineCarouselViewTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun headline(title: String, link: String? = "https://example.com/$title") =
        Headline(feedTitle = "feed", title = title, link = link, published = null)

    private fun visibleTitle(view: HeadlineCarouselView): String? = view.visibleHeadline?.title

    @Test
    fun `setHeadlines shows the first headline immediately`() {
        val view = HeadlineCarouselView(context(), FakePeriodicScheduler())

        view.setHeadlines(listOf(headline("h1"), headline("h2"), headline("h3")))

        assertEquals("h1", visibleTitle(view))
    }

    @Test
    fun `a tick advances to the next headline, wrapping around`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.setHeadlines(listOf(headline("h1"), headline("h2"), headline("h3")))

        fake.fireTick()
        assertEquals("h2", visibleTitle(view))

        fake.fireTick()
        assertEquals("h3", visibleTitle(view))

        fake.fireTick()
        assertEquals("h1", visibleTitle(view))
    }

    @Test
    fun `setHeadlines with a new list restarts from index 0 but leaves the visible child untouched until the next tick`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.setHeadlines(listOf(headline("a1"), headline("a2")))
        fake.fireTick()
        assertEquals("a2", visibleTitle(view))

        view.setHeadlines(listOf(headline("b1"), headline("b2")))
        assertEquals("a2", visibleTitle(view))

        fake.fireTick()
        assertEquals("b1", visibleTitle(view))
    }

    @Test
    fun `setHeadlines with an unchanged list is a no-op, not resetting the rotation`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        val list = listOf(headline("a1"), headline("a2"))
        view.setHeadlines(list)
        fake.fireTick()
        assertEquals("a2", visibleTitle(view))

        view.setHeadlines(listOf(headline("a1"), headline("a2")))
        assertEquals("a2", visibleTitle(view))

        fake.fireTick()
        assertEquals("a1", visibleTitle(view))
    }

    @Test
    fun `tapping fires onHeadlineClick with the visible headline`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        val h1 = headline("h1")
        view.setHeadlines(listOf(h1, headline("h2")))

        var clicked: Headline? = null
        view.onHeadlineClick = { clicked = it }
        view.performClick()

        assertEquals(h1, clicked)
    }

    @Test
    fun `a headline without a link is not clickable`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.setHeadlines(listOf(headline("h1", link = null)))

        var clicked: Headline? = null
        view.onHeadlineClick = { clicked = it }
        view.performClick()

        assertNull(clicked)
    }

    @Test
    fun `a single headline does not start the scheduler`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)

        view.setHeadlines(listOf(headline("only")))

        assertEquals("only", visibleTitle(view))
        assertFalse(fake.isRunning)
    }

    @Test
    fun `after a crossfade animation finishes exactly one child view is left visible`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.setHeadlines(listOf(headline("h1"), headline("h2")))

        fake.fireTick()
        shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS)

        val visibleChildren = (0 until view.childCount).count { i ->
            val child = view.getChildAt(i)
            child.visibility == View.VISIBLE && child.alpha > 0.5f
        }
        assertEquals(1, visibleChildren)
    }

    @Test
    fun `empty headlines shows the placeholder and tap opens nothing`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)

        view.setHeadlines(emptyList())

        assertNull(view.visibleHeadline)
        assertTrue(view.isShowingPlaceholder)

        var clicked: Headline? = null
        view.onHeadlineClick = { clicked = it }
        view.performClick()
        assertNull(clicked)
    }

    @Test
    fun `pauseRotation stops a running rotation`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.setHeadlines(listOf(headline("h1"), headline("h2")))

        view.pauseRotation()

        assertFalse(fake.isRunning)
    }

    @Test
    fun `resumeRotation restarts rotation for the unchanged list`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        val list = listOf(headline("h1"), headline("h2"))
        view.setHeadlines(list)
        view.pauseRotation()
        view.setHeadlines(list.toList())

        view.resumeRotation()

        assertTrue(fake.isRunning)
        fake.fireTick()
        assertEquals("h2", visibleTitle(view))
    }

    @Test
    fun `resumeRotation with a single headline does not start the scheduler`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.setHeadlines(listOf(headline("only")))
        view.pauseRotation()

        view.resumeRotation()

        assertFalse(fake.isRunning)
    }

    @Test
    fun `setHeadlines while paused shows the first headline but does not start rotating`() {
        val fake = FakePeriodicScheduler()
        val view = HeadlineCarouselView(context(), fake)
        view.pauseRotation()

        view.setHeadlines(listOf(headline("h1"), headline("h2")))

        assertEquals("h1", visibleTitle(view))
        assertFalse(fake.isRunning)
    }

    @Test
    fun `detaching from the window stops the rotation`() {
        val fake = FakePeriodicScheduler()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = HeadlineCarouselView(activity, fake)
        activity.setContentView(view)
        view.setHeadlines(listOf(headline("h1"), headline("h2")))
        assertTrue(fake.isRunning)

        (view.parent as android.view.ViewGroup).removeView(view)

        assertFalse(fake.isRunning)
    }
}
