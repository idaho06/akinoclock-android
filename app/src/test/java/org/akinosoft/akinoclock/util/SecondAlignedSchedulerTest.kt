package org.akinosoft.akinoclock.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SecondAlignedSchedulerTest {

    private fun newScheduler(): SecondAlignedScheduler =
        SecondAlignedScheduler(Handler(Looper.getMainLooper()), nowMillis = { SystemClock.uptimeMillis() })

    @Test
    fun `delay to next second boundary`() {
        val handler = Handler(Looper.getMainLooper())
        val scheduler = SecondAlignedScheduler(handler, nowMillis = { 12_345L })
        assertEquals(655L, scheduler.delayToNextSecondMillis())
    }

    @Test
    fun `one second of looper time fires one tick`() {
        val scheduler = newScheduler()
        var ticks = 0
        scheduler.start { ticks++ }

        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertEquals(1, ticks)
    }

    @Test
    fun `three seconds of looper time fires three ticks`() {
        val scheduler = newScheduler()
        var ticks = 0
        scheduler.start { ticks++ }

        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        assertEquals(3, ticks)
    }

    @Test
    fun `stop prevents further ticks`() {
        val scheduler = newScheduler()
        var ticks = 0
        scheduler.start { ticks++ }
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertEquals(1, ticks)

        scheduler.stop()
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        assertEquals(1, ticks)
    }

    @Test
    fun `starting twice does not double schedule`() {
        val scheduler = newScheduler()
        var firstTicks = 0
        var secondTicks = 0
        scheduler.start { firstTicks++ }
        scheduler.start { secondTicks++ }

        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        assertEquals(1, firstTicks)
        assertEquals(0, secondTicks)
    }
}
