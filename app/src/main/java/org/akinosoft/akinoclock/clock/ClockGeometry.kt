package org.akinosoft.akinoclock.clock

import kotlin.math.cos
import kotlin.math.sin

data class Point(val x: Float, val y: Float)

data class TickLine(val outer: Point, val inner: Point)

data class HandRect(val width: Float, val tailY: Float, val tipY: Float)

data class TipSegment(val startY: Float, val endY: Float)

enum class HandKind { HOUR, MINUTE, SECOND }

/**
 * Pure geometry for the dial: all functions take a radius `r` and return simple data,
 * with (0, 0) as the dial center and 12 o'clock at angle 0, clockwise positive.
 */
object ClockGeometry {

    private const val TICK_OUTER_RATIO = 0.95f
    private const val TICK_INNER_RATIO = 0.90f
    private const val NUMERAL_RADIUS_RATIO = 0.72f
    private const val CENTER_CAP_RATIO = 0.045f

    fun minorTickIndices(): List<Int> = (0..59).filter { it % 5 != 0 }

    private fun pointOnCircle(angleDeg: Double, radius: Float): Point {
        val angleRad = Math.toRadians(angleDeg)
        return Point(radius * sin(angleRad).toFloat(), -radius * cos(angleRad).toFloat())
    }

    fun tickLine(index: Int, r: Float): TickLine {
        require(index % 5 != 0) { "index $index is an hour position; no tick is drawn there" }
        val angleDeg = index * 6.0
        val outer = pointOnCircle(angleDeg, r * TICK_OUTER_RATIO)
        val inner = pointOnCircle(angleDeg, r * TICK_INNER_RATIO)
        return TickLine(outer, inner)
    }

    fun minorTickLines(r: Float): FloatArray {
        val indices = minorTickIndices()
        val result = FloatArray(indices.size * 4)
        indices.forEachIndexed { i, index ->
            val line = tickLine(index, r)
            result[i * 4] = line.outer.x
            result[i * 4 + 1] = line.outer.y
            result[i * 4 + 2] = line.inner.x
            result[i * 4 + 3] = line.inner.y
        }
        return result
    }

    fun numeralCenter(hour12: Int, r: Float): Point =
        pointOnCircle((hour12 % 12) * 30.0, r * NUMERAL_RADIUS_RATIO)

    fun handRect(kind: HandKind, r: Float): HandRect = when (kind) {
        HandKind.HOUR -> HandRect(width = 0.06f * r, tailY = 0.08f * r, tipY = -0.50f * r)
        HandKind.MINUTE -> HandRect(width = 0.05f * r, tailY = 0.08f * r, tipY = -0.74f * r)
        HandKind.SECOND -> HandRect(width = 0.012f * r, tailY = 0.18f * r, tipY = -0.80f * r)
    }

    fun tipSegment(kind: HandKind, r: Float): TipSegment {
        require(kind != HandKind.SECOND) { "the second hand has no tip color segment" }
        val rect = handRect(kind, r)
        val tipLength = if (kind == HandKind.HOUR) 0.18f * r else 0.22f * r
        return TipSegment(startY = rect.tipY, endY = rect.tipY + tipLength)
    }

    fun centerCapRadius(r: Float): Float = CENTER_CAP_RATIO * r
}
