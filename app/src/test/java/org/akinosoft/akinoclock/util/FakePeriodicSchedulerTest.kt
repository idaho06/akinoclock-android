package org.akinosoft.akinoclock.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePeriodicSchedulerTest {

    @Test
    fun `fake records start and stop calls`() {
        val fake = FakePeriodicScheduler()
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
        val fake = FakePeriodicScheduler()
        var ticks = 0
        fake.start { ticks++ }

        fake.fireTick()
        fake.fireTick()
        assertEquals(2, ticks)

        fake.stop()
        fake.fireTick()
        assertEquals(2, ticks)
    }
}
