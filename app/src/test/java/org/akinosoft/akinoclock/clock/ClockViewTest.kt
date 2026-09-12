package org.akinosoft.akinoclock.clock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.R
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.akinosoft.akinoclock.util.FakePeriodicScheduler
import org.akinosoft.akinoclock.util.PeriodicScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

private class MutableClock(
    private var current: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
    fun advanceTo(instant: Instant) {
        current = instant
    }
}

private class TestableClockView(
    context: Context,
    clock: Clock,
    tickScheduler: PeriodicScheduler,
) : ClockView(context, clock, tickScheduler) {
    var invalidateCount = 0
        private set

    override fun invalidate() {
        invalidateCount++
        super.invalidate()
    }
}

@RunWith(RobolectricTestRunner::class)
class ClockViewTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun fixedClock(hour: Int, minute: Int, second: Int): Clock =
        Clock.fixed(LocalDateTime.of(2024, 1, 1, hour, minute, second).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun mutableClock(hour: Int, minute: Int, second: Int): MutableClock =
        MutableClock(LocalDateTime.of(2024, 1, 1, hour, minute, second).toInstant(ZoneOffset.UTC))

    private fun attachToActivity(view: View): ActivityController<Activity> {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.create()
        controller.get().setContentView(view)
        controller.start().resume().visible()
        return controller
    }

    @Test
    fun `constructing with a fixed clock sets time from that clock`() {
        val view = TestableClockView(context(), fixedClock(9, 45, 30), FakePeriodicScheduler())
        assertEquals(ClockTime(9, 45, 30), view.time)
    }

    @Test
    fun `attaching to window starts the scheduler, detaching stops it`() {
        val fake = FakePeriodicScheduler()
        val view = TestableClockView(context(), fixedClock(0, 0, 0), fake)
        val controller = attachToActivity(view)
        assertTrue(fake.isRunning)

        controller.pause().stop().destroy()
        assertFalse(fake.isRunning)
    }

    @Test
    fun `tick advances time and invalidates`() {
        val fake = FakePeriodicScheduler()
        val clock = mutableClock(10, 0, 0)
        val view = TestableClockView(context(), clock, fake)
        view.start()

        clock.advanceTo(LocalDateTime.of(2024, 1, 1, 10, 0, 1).toInstant(ZoneOffset.UTC))
        fake.fireTick()

        assertEquals(ClockTime(10, 0, 1), view.time)
        assertTrue(view.invalidateCount > 0)
    }

    @Test
    fun `palette alarmHand is bright in the light theme and dark in the dark theme`() {
        RuntimeEnvironment.setQualifiers("+notnight")
        val lightAlarmHand = context().getColor(R.color.alarm_hand)

        RuntimeEnvironment.setQualifiers("+night")
        val nightAlarmHand = context().getColor(R.color.alarm_hand)

        assertTrue(luminance(lightAlarmHand) > luminance(nightAlarmHand))
    }

    private fun luminance(color: Int): Int = Color.red(color) + Color.green(color) + Color.blue(color)

    @Test
    fun `dial renders background color and yellow second hand`() {
        val view = TestableClockView(context(), fixedClock(12, 0, 15), FakePeriodicScheduler())
        view.layout(0, 0, 200, 200)

        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val palette = view.palette
        assertEquals(palette.dialBackground, bitmap.getPixel(90, 90))

        val secondHandNeighborhood = (95..105).map { y -> bitmap.getPixel(160, y) }
        val closestMatch = secondHandNeighborhood.minByOrNull { colorDistance(it, palette.secondHand) }!!
        assertTrue(colorDistance(closestMatch, palette.secondHand) < colorDistance(closestMatch, palette.dialBackground))
    }

    private fun colorDistance(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return dr * dr + dg * dg + db * db
    }

    /** Nearest match to `target` in a square neighborhood around (cx, cy), radius in pixels. */
    private fun closestInNeighborhood(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, target: Int): Int {
        var best = bitmap.getPixel(cx, cy)
        var bestDistance = colorDistance(best, target)
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                val x = cx + dx
                val y = cy + dy
                if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) continue
                val candidate = bitmap.getPixel(x, y)
                val distance = colorDistance(candidate, target)
                if (distance < bestDistance) {
                    best = candidate
                    bestDistance = distance
                }
            }
        }
        return best
    }

    @Test
    fun `alarm hand pill is drawn straight down when the alarm is 1 hour away`() {
        val clock = fixedClock(5, 0, 0)
        val view = TestableClockView(context(), clock, FakePeriodicScheduler())
        view.nextAlarm = LocalDateTime.of(2024, 1, 1, 6, 0, 0).toInstant(ZoneOffset.UTC)
        view.layout(0, 0, 200, 200)

        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val palette = view.palette
        val closest = closestInNeighborhood(bitmap, cx = 100, cy = 156, radius = 3, target = palette.handTip)
        assertTrue(colorDistance(closest, palette.handTip) < colorDistance(closest, palette.dialBackground))
    }

    @Test
    fun `alarm hand is hidden when the alarm is more than 12 hours away`() {
        val clock = fixedClock(5, 0, 0)
        val view = TestableClockView(context(), clock, FakePeriodicScheduler())
        view.nextAlarm = LocalDateTime.of(2024, 1, 1, 18, 30, 0).toInstant(ZoneOffset.UTC)
        view.layout(0, 0, 200, 200)

        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val palette = view.palette
        val closest = closestInNeighborhood(bitmap, cx = 100, cy = 156, radius = 3, target = palette.handTip)
        assertTrue(colorDistance(closest, palette.dialBackground) < colorDistance(closest, palette.handTip))
    }

    @Test
    fun `alarm hand is hidden when there is no next alarm`() {
        val view = TestableClockView(context(), fixedClock(5, 0, 0), FakePeriodicScheduler())
        view.layout(0, 0, 200, 200)

        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val palette = view.palette
        val closest = closestInNeighborhood(bitmap, cx = 100, cy = 156, radius = 3, target = palette.handTip)
        assertTrue(colorDistance(closest, palette.dialBackground) < colorDistance(closest, palette.handTip))
    }

    @Test
    fun `alarm hand appears from time passing alone, crossing the 12 hour window without touching nextAlarm`() {
        val fake = FakePeriodicScheduler()
        val clock = mutableClock(5, 0, 0)
        val view = TestableClockView(context(), clock, fake)
        view.nextAlarm = LocalDateTime.of(2024, 1, 1, 18, 30, 0).toInstant(ZoneOffset.UTC)
        view.layout(0, 0, 200, 200)
        view.start()

        // At 195 degrees (18:30 on a 12-hour dial), the pill's mid-radius (0.56r) point is at
        // (cx + 100*sin(195deg), cy - 100*cos(195deg)) = (100 - 14.5, 100 + 54.1).
        val pillX = 86
        val pillY = 154

        var bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        var palette = view.palette
        var closest = closestInNeighborhood(bitmap, pillX, pillY, radius = 3, target = palette.handTip)
        assertTrue(colorDistance(closest, palette.dialBackground) < colorDistance(closest, palette.handTip))

        // Advance to 08:00 (10.5h from the alarm), comfortably past the 12h boundary.
        clock.advanceTo(LocalDateTime.of(2024, 1, 1, 8, 0, 0).toInstant(ZoneOffset.UTC))
        fake.fireTick()

        bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        palette = view.palette
        closest = closestInNeighborhood(bitmap, pillX, pillY, radius = 3, target = palette.handTip)
        assertTrue(colorDistance(closest, palette.handTip) < colorDistance(closest, palette.dialBackground))
    }

    @Test
    fun `content description appends the alarm time while the hand is shown`() {
        val view = TestableClockView(context(), fixedClock(5, 0, 0), FakePeriodicScheduler())
        assertEquals("05:00", view.contentDescription)

        view.nextAlarm = LocalDateTime.of(2024, 1, 1, 6, 0, 0).toInstant(ZoneOffset.UTC)
        assertEquals("05:00, alarm 06:00", view.contentDescription)

        view.nextAlarm = null
        assertEquals("05:00", view.contentDescription)
    }

    @Test
    fun `setting nextAlarm invalidates exactly once`() {
        val view = TestableClockView(context(), fixedClock(5, 0, 0), FakePeriodicScheduler())
        val before = view.invalidateCount

        view.nextAlarm = LocalDateTime.of(2024, 1, 1, 6, 0, 0).toInstant(ZoneOffset.UTC)

        assertEquals(before + 1, view.invalidateCount)
    }

    @Test
    fun `visibility GONE stops ticking, VISIBLE restarts it`() {
        val fake = FakePeriodicScheduler()
        val view = TestableClockView(context(), fixedClock(0, 0, 0), fake)
        attachToActivity(view)
        assertTrue(fake.isRunning)

        view.visibility = View.GONE
        assertFalse(fake.isRunning)

        view.visibility = View.VISIBLE
        assertTrue(fake.isRunning)
    }

    @Test
    fun `content description reflects HH mm`() {
        val view = TestableClockView(context(), fixedClock(12, 34, 56), FakePeriodicScheduler())
        assertEquals("12:34", view.contentDescription)
    }

    @Test
    fun `content description reflects the injected clock even when its minute matches the real wall clock`() {
        // The `clock` field defaults to Clock.systemDefaultZone() before the secondary
        // constructor installs the injected clock, so a naive minute-change guard on the
        // content description could keep the real-time string if the real minute happens
        // to match the injected clock's minute at construction time.
        val realNow = java.time.LocalTime.now()
        val collidingHour = (realNow.hour + 3) % 24
        val view = TestableClockView(context(), fixedClock(collidingHour, realNow.minute, 0), FakePeriodicScheduler())

        val expected = String.format("%02d:%02d", collidingHour, realNow.minute)
        assertEquals(expected, view.contentDescription)
    }

    @Test
    fun `timezone change broadcast updates time while attached, not after detach`() {
        val fake = FakePeriodicScheduler()
        val clock = mutableClock(8, 0, 0)
        val view = TestableClockView(context(), clock, fake)
        val controller = attachToActivity(view)
        val activity = controller.get()

        clock.advanceTo(LocalDateTime.of(2024, 1, 1, 9, 15, 0).toInstant(ZoneOffset.UTC))
        activity.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(ClockTime(9, 15, 0), view.time)
        assertTrue(view.invalidateCount > 0)

        controller.pause().stop().destroy()

        clock.advanceTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0).toInstant(ZoneOffset.UTC))
        activity.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(ClockTime(9, 15, 0), view.time)
    }

    @Test
    fun `measure passes through when both dimensions are EXACTLY`() {
        val view = TestableClockView(context(), fixedClock(0, 0, 0), FakePeriodicScheduler())
        view.measure(
            MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY),
        )
        assertEquals(500, view.measuredWidth)
        assertEquals(300, view.measuredHeight)
    }

    @Test
    fun `measure with two AT_MOST bounds chooses the largest square that fits`() {
        val view = TestableClockView(context(), fixedClock(0, 0, 0), FakePeriodicScheduler())
        view.measure(
            MeasureSpec.makeMeasureSpec(440, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(600, MeasureSpec.AT_MOST),
        )
        assertEquals(440, view.measuredWidth)
        assertEquals(440, view.measuredHeight)
    }

    @Test
    fun `measure with both dimensions UNSPECIFIED uses the default square size`() {
        val view = TestableClockView(context(), fixedClock(0, 0, 0), FakePeriodicScheduler())
        view.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        val expected = context().resources.getDimensionPixelSize(R.dimen.clock_default_size)
        assertEquals(expected, view.measuredWidth)
        assertEquals(expected, view.measuredHeight)
    }

    @Test
    fun `measure with an exact width and unspecified height sizes a square to the exact dimension`() {
        val view = TestableClockView(context(), fixedClock(0, 0, 0), FakePeriodicScheduler())
        view.measure(
            MeasureSpec.makeMeasureSpec(350, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        assertEquals(350, view.measuredWidth)
        assertEquals(350, view.measuredHeight)
    }
}
