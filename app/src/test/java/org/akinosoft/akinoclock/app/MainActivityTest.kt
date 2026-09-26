package org.akinosoft.akinoclock.app

import android.os.Looper
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.calendar.model.EventInstance
import org.akinosoft.akinoclock.clock.alarm.NextAlarmSource
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.ui.SettingsActivity
import org.akinosoft.akinoclock.util.FakePeriodicScheduler
import org.akinosoft.akinoclock.util.FakeSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeCalendarRepository : CalendarRepository {
    var activeCollectors = 0
        private set

    override suspend fun instancesBetween(begin: Long, end: Long): List<EventInstance> = emptyList()

    override fun changes(): Flow<Unit> = flow {
        activeCollectors++
        try {
            awaitCancellation()
        } finally {
            activeCollectors--
        }
    }
}

private class FakeNextAlarmSource : NextAlarmSource {
    private val state = MutableStateFlow<Instant?>(null)

    fun setNextAlarm(value: Instant?) {
        state.value = value
    }

    override fun nextAlarm(): Instant? = state.value
    override fun changes(): Flow<Instant?> = state
}

private fun fakeRssRepository(headlines: List<Headline> = emptyList()): RssRepository = mockk {
    every { headlines() } returns MutableStateFlow(headlines)
    every { status() } returns MutableStateFlow(emptyMap())
    coEvery { refresh(any()) } returns RefreshOutcome.SUCCESS
}

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private fun installFakeContainer(
        calendarRepository: CalendarRepository = FakeCalendarRepository(),
        rssRepository: RssRepository = fakeRssRepository(),
        settingsRepository: SettingsRepository = FakeSettingsRepository(
            initialFeeds = listOf(FeedConfig(url = "https://example.com/feed.xml")),
        ),
        permissionChecker: PermissionChecker = mockk { every { hasReadCalendar() } returns true },
        nextAlarmSource: NextAlarmSource = FakeNextAlarmSource(),
    ) {
        val app = ApplicationProvider.getApplicationContext<AkinoClockApp>()
        app.container = AppContainer(
            context = app,
            clock = Clock.systemDefaultZone(),
            calendarRepository = calendarRepository,
            permissionChecker = permissionChecker,
            settingsRepository = settingsRepository,
            rssRepository = rssRepository,
            nextAlarmSource = nextAlarmSource,
        )
    }

    @Test
    fun `FLAG_KEEP_SCREEN_ON is set on the window`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        val flags = controller.get().window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
    }

    @Test
    fun `starting the activity subscribes to calendar changes, stopping unsubscribes`() {
        val calendarRepository = FakeCalendarRepository()
        installFakeContainer(calendarRepository = calendarRepository)
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        controller.start()
        assertEquals(1, calendarRepository.activeCollectors)

        controller.stop()
        assertEquals(0, calendarRepository.activeCollectors)
    }

    @Test
    fun `starting the activity reflects the next alarm on the clock view, stopping ignores updates, resuming re-syncs`() {
        val nextAlarmSource = FakeNextAlarmSource()
        installFakeContainer(nextAlarmSource = nextAlarmSource)
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()

        controller.start()
        val alarm = Instant.parse("2024-01-01T06:00:00Z")
        nextAlarmSource.setNextAlarm(alarm)
        assertEquals(alarm, activity.binding.clockView.nextAlarm)

        controller.stop()
        nextAlarmSource.setNextAlarm(null)
        assertEquals(alarm, activity.binding.clockView.nextAlarm)

        controller.start()
        assertEquals(null, activity.binding.clockView.nextAlarm)
    }

    @Test
    fun `starting the activity triggers an rss refresh`() {
        val rssRepository = fakeRssRepository()
        installFakeContainer(rssRepository = rssRepository)
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        controller.start()

        coVerify(atLeast = 1) { rssRepository.refresh(any()) }
    }

    @Test
    fun `resuming starts the clock scheduler, pausing stops it`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start()
        val activity = controller.get()
        val fakeScheduler = FakePeriodicScheduler()
        activity.binding.clockView.tickScheduler = fakeScheduler

        controller.resume()
        assertTrue(fakeScheduler.isRunning)

        controller.pause()
        assertFalse(fakeScheduler.isRunning)
    }

    @Test
    fun `starting the activity runs the headline rotation, stopping pauses it`() {
        val headlines = listOf("h1", "h2").map { Headline("feed", it, "https://example.com/$it", null) }
        installFakeContainer(rssRepository = fakeRssRepository(headlines))
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val fakeScheduler = FakePeriodicScheduler()
        controller.get().binding.rssCarousel.scheduler = fakeScheduler

        controller.start()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fakeScheduler.isRunning)

        controller.stop()
        assertFalse(fakeScheduler.isRunning)

        controller.start()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fakeScheduler.isRunning)
    }

    @Test
    @Config(qualifiers = "land")
    fun `landscape configuration inflates the horizontal layout`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        val root = controller.get().binding.root as LinearLayout
        assertEquals(LinearLayout.HORIZONTAL, root.orientation)
    }

    private fun assertDialSitsBehindClock(activity: MainActivity) {
        val dial = activity.binding.dialView
        val hands = activity.binding.clockView
        val parent = hands.parent as android.view.ViewGroup
        assertTrue(dial.parent === parent)
        assertTrue(parent.indexOfChild(dial) < parent.indexOfChild(hands))
    }

    @Test
    fun `the static dial sits behind the clock hands in portrait`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        assertDialSitsBehindClock(controller.get())
    }

    @Test
    @Config(qualifiers = "land")
    fun `the static dial sits behind the clock hands in landscape`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()

        assertDialSitsBehindClock(controller.get())
    }

    @Test
    fun `recreating the activity does not throw and re-renders`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()

        controller.recreate()
    }

    @Test
    fun `tapping the settings gear launches SettingsActivity`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()

        activity.binding.settingsButton.performClick()

        val next = shadowOf(activity).nextStartedActivity
        assertEquals(SettingsActivity::class.java.name, next.component?.className)
    }
}
