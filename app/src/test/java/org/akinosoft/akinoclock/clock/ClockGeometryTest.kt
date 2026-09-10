package org.akinosoft.akinoclock.clock

import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockGeometryTest {

    private val r = 100f

    @Test
    fun `minorTickIndices excludes hour-aligned multiples of 5`() {
        val indices = ClockGeometry.minorTickIndices()
        assertEquals(48, indices.size)
        assertTrue(0 !in indices)
        assertTrue(5 !in indices)
        assertTrue(1 in indices)
        assertTrue(indices.all { it % 5 != 0 })
    }

    @Test
    fun `hourTickIndices are the 12 multiples of 5`() {
        val indices = ClockGeometry.hourTickIndices()
        assertEquals(12, indices.size)
        assertTrue(0 in indices)
        assertTrue(55 in indices)
        assertTrue(indices.all { it % 5 == 0 })
    }

    @Test
    fun `tickLine at an hour-aligned index is longer than a minor tick`() {
        val line = ClockGeometry.tickLine(0, r)
        assertEquals(0f, line.outer.x, 0.01f)
        assertEquals(-95f, line.outer.y, 0.01f)
        assertEquals(0f, line.inner.x, 0.01f)
        assertEquals(-85f, line.inner.y, 0.01f)
    }

    @Test
    fun `tickLine at index 5 (1 o'clock hour position)`() {
        val line = ClockGeometry.tickLine(5, r)
        assertEquals(47.5f, line.outer.x, 0.01f)
        assertEquals(-82.27f, line.outer.y, 0.01f)
        assertEquals(42.5f, line.inner.x, 0.01f)
        assertEquals(-73.61f, line.inner.y, 0.01f)
    }

    @Test
    fun `hourTickLines produces a flat array of 12 ticks`() {
        val lines = ClockGeometry.hourTickLines(r)
        assertEquals(12 * 4, lines.size)
    }

    @Test
    fun `tickLine index 1`() {
        val line = ClockGeometry.tickLine(1, r)
        assertEquals(9.93f, line.outer.x, 0.01f)
        assertEquals(-94.48f, line.outer.y, 0.01f)
        assertEquals(9.41f, line.inner.x, 0.01f)
        assertEquals(-89.51f, line.inner.y, 0.01f)
    }

    @Test
    fun `tickLine index 4`() {
        val line = ClockGeometry.tickLine(4, r)
        assertEquals(38.64f, line.outer.x, 0.01f)
        assertEquals(-86.79f, line.outer.y, 0.01f)
        assertEquals(36.61f, line.inner.x, 0.01f)
        assertEquals(-82.22f, line.inner.y, 0.01f)
    }

    @Test
    fun `minorTickLines produces a flat array of 48 ticks`() {
        val lines = ClockGeometry.minorTickLines(r)
        assertEquals(48 * 4, lines.size)
    }

    @Test
    fun `numeralCenter 12 is straight up`() {
        val p = ClockGeometry.numeralCenter(12, r)
        assertEquals(0f, p.x, 0.01f)
        assertEquals(-72f, p.y, 0.01f)
    }

    @Test
    fun `numeralCenter 3 is straight right`() {
        val p = ClockGeometry.numeralCenter(3, r)
        assertEquals(72f, p.x, 0.01f)
        assertEquals(0f, p.y, 0.01f)
    }

    @Test
    fun `numeralCenter angle matches the suppressed hour tick index`() {
        for (hour in 1..12) {
            val p = ClockGeometry.numeralCenter(hour, r)
            val angleRad = Math.toRadians((hour % 12) * 30.0)
            val expectedX = (r * 0.72f) * sin(angleRad).toFloat()
            val expectedY = -(r * 0.72f) * cos(angleRad).toFloat()
            assertEquals(expectedX, p.x, 0.01f)
            assertEquals(expectedY, p.y, 0.01f)
        }
    }

    @Test
    fun `handRect for hour hand`() {
        val rect = ClockGeometry.handRect(HandKind.HOUR, r)
        assertEquals(6f, rect.width, 0.01f)
        assertEquals(8f, rect.tailY, 0.01f)
        assertEquals(-50f, rect.tipY, 0.01f)
    }

    @Test
    fun `handRect for minute hand`() {
        val rect = ClockGeometry.handRect(HandKind.MINUTE, r)
        assertEquals(5f, rect.width, 0.01f)
        assertEquals(8f, rect.tailY, 0.01f)
        assertEquals(-74f, rect.tipY, 0.01f)
    }

    @Test
    fun `handRect for second hand`() {
        val rect = ClockGeometry.handRect(HandKind.SECOND, r)
        assertEquals(1.2f, rect.width, 0.01f)
        assertEquals(18f, rect.tailY, 0.01f)
        assertEquals(-80f, rect.tipY, 0.01f)
    }

    @Test
    fun `tipSegment for hour hand has a margin on both ends and is thinner than the baton`() {
        val tip = ClockGeometry.tipSegment(HandKind.HOUR, r)
        assertEquals(-47f, tip.startY, 0.01f)
        assertEquals(-35f, tip.endY, 0.01f)
        assertEquals(3.6f, tip.width, 0.01f)
    }

    @Test
    fun `tipSegment for minute hand has a margin on both ends and is thinner than the baton`() {
        val tip = ClockGeometry.tipSegment(HandKind.MINUTE, r)
        assertEquals(-71f, tip.startY, 0.01f)
        assertEquals(-55f, tip.endY, 0.01f)
        assertEquals(3f, tip.width, 0.01f)
    }

    @Test
    fun `tipSegment for second hand throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClockGeometry.tipSegment(HandKind.SECOND, r)
        }
    }

    @Test
    fun `centerCapRadius is 0-045R`() {
        assertEquals(4.5f, ClockGeometry.centerCapRadius(r), 0.01f)
    }
}
