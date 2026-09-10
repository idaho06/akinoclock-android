package org.akinosoft.akinoclock.clock

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TickSchedulerTest {

    private fun newScheduler(): HandlerTickScheduler =
        HandlerTickScheduler(Handler(Looper.getMainLooper()), nowMillis = { SystemClock.uptimeMillis() })

    // --- FakeTickScheduler ---

    @Test
    fun `fake records start and stop calls`() {
        val fake = FakeTickScheduler()
        assertFalse(fake.isRunning)

        fake.start {}
        assertTrue(fake.isRunning)
        assertEquals(1, fake.startCount)

        fake.stop()
        assertFalse(fake.isRunning)
        assertEquals(1, fake.stopCount)
    }

    @Test
    fun `fake fires ticks only while running`() {
        val fake = FakeTickScheduler()
        var ticks = 0
        fake.start { ticks++ }

        fake.fireTick()
        fake.fireTick()
        assertEquals(2, ticks)

        fake.stop()
        fake.fireTick()
        assertEquals(2, ticks)
    }

    // --- HandlerTickScheduler ---

    @Test
    fun `delay to next second boundary`() {
        val handler = Handler(Looper.getMainLooper())
        val scheduler = HandlerTickScheduler(handler, nowMillis = { 12_345L })
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
