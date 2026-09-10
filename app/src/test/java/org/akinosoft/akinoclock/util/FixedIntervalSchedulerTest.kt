package org.akinosoft.akinoclock.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FixedIntervalSchedulerTest {

    private fun newScheduler(intervalMillis: Long = 8_000L): FixedIntervalScheduler =
        FixedIntervalScheduler(Handler(Looper.getMainLooper()), intervalMillis)

    @Test
    fun `does not tick before the interval elapses`() {
        val scheduler = newScheduler()
        var ticks = 0
        scheduler.start { ticks++ }

        shadowOf(Looper.getMainLooper()).idleFor(7_999, TimeUnit.MILLISECONDS)

        assertEquals(0, ticks)
    }

    @Test
    fun `ticks once per interval, with no wall-clock alignment`() {
        val scheduler = newScheduler()
        var ticks = 0
        scheduler.start { ticks++ }

        shadowOf(Looper.getMainLooper()).idleFor(8_000, TimeUnit.MILLISECONDS)
        assertEquals(1, ticks)

        shadowOf(Looper.getMainLooper()).idleFor(24_000, TimeUnit.MILLISECONDS)
        assertEquals(4, ticks)
    }

    @Test
    fun `stop prevents further ticks`() {
        val scheduler = newScheduler()
        var ticks = 0
        scheduler.start { ticks++ }
        shadowOf(Looper.getMainLooper()).idleFor(8_000, TimeUnit.MILLISECONDS)
        assertEquals(1, ticks)

        scheduler.stop()
        shadowOf(Looper.getMainLooper()).idleFor(40_000, TimeUnit.MILLISECONDS)
        assertEquals(1, ticks)
    }

    @Test
    fun `starting twice does not double schedule`() {
        val scheduler = newScheduler()
        var firstTicks = 0
        var secondTicks = 0
        scheduler.start { firstTicks++ }
        scheduler.start { secondTicks++ }

        shadowOf(Looper.getMainLooper()).idleFor(8_000, TimeUnit.MILLISECONDS)

        assertEquals(1, firstTicks)
        assertEquals(0, secondTicks)
    }
}
