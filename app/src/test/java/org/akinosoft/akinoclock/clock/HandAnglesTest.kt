package org.akinosoft.akinoclock.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HandAnglesTest {

    @Test
    fun `12 00 00 all hands at zero`() {
        val angles = ClockTime(12, 0, 0).toHandAngles()
        assertEquals(0f, angles.hourDeg, 0.01f)
        assertEquals(0f, angles.minuteDeg, 0.01f)
        assertEquals(0f, angles.secondDeg, 0.01f)
    }

    @Test
    fun `03 00 00 hour at 90`() {
        val angles = ClockTime(3, 0, 0).toHandAngles()
        assertEquals(90f, angles.hourDeg, 0.01f)
    }

    @Test
    fun `06 30 00 hour at 195 minute at 180`() {
        val angles = ClockTime(6, 30, 0).toHandAngles()
        assertEquals(195f, angles.hourDeg, 0.01f)
        assertEquals(180f, angles.minuteDeg, 0.01f)
    }

    @Test
    fun `09 45 30 hour minute second precise`() {
        val angles = ClockTime(9, 45, 30).toHandAngles()
        assertEquals(292.75f, angles.hourDeg, 0.01f)
        assertEquals(273f, angles.minuteDeg, 0.01f)
        assertEquals(180f, angles.secondDeg, 0.01f)
    }

    @Test
    fun `15 00 00 hour wraps 24h to 90`() {
        val angles = ClockTime(15, 0, 0).toHandAngles()
        assertEquals(90f, angles.hourDeg, 0.01f)
    }

    @Test
    fun `23 59 59 near full rotation`() {
        val angles = ClockTime(23, 59, 59).toHandAngles()
        assertEquals(359.99f, angles.hourDeg, 0.01f)
        assertEquals(359.9f, angles.minuteDeg, 0.01f)
        assertEquals(354f, angles.secondDeg, 0.01f)
    }

    @Test
    fun `second angle is always a multiple of 6`() {
        for (second in 0..59) {
            val angles = ClockTime(0, 0, second).toHandAngles()
            assertEquals(0f, angles.secondDeg % 6f, 0.001f)
        }
    }

    @Test
    fun `hour 24 throws`() {
        assertThrows(IllegalArgumentException::class.java) { ClockTime(24, 0, 0) }
    }

    @Test
    fun `minute 60 throws`() {
        assertThrows(IllegalArgumentException::class.java) { ClockTime(0, 60, 0) }
    }

    @Test
    fun `second 61 throws`() {
        assertThrows(IllegalArgumentException::class.java) { ClockTime(0, 0, 61) }
    }
}
