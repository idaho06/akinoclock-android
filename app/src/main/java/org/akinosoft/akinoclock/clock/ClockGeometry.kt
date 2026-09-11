package org.akinosoft.akinoclock.clock

import kotlin.math.cos
import kotlin.math.sin

data class Point(val x: Float, val y: Float)

data class TickLine(val outer: Point, val inner: Point)

data class HandRect(val width: Float, val tailY: Float, val tipY: Float)

data class TipSegment(val startY: Float, val endY: Float, val width: Float)

enum class HandKind { HOUR, MINUTE, SECOND, ALARM }

/**
 * Pure geometry for the dial: all functions take a radius `r` and return simple data,
 * with (0, 0) as the dial center and 12 o'clock at angle 0, clockwise positive.
 */
object ClockGeometry {

    private const val TICK_OUTER_RATIO = 0.95f
    private const val MINOR_TICK_INNER_RATIO = 0.90f
    private const val HOUR_TICK_INNER_RATIO = 0.85f
    private const val NUMERAL_RADIUS_RATIO = 0.72f
    private const val CENTER_CAP_RATIO = 0.045f
    private const val TIP_MARGIN_RATIO = 0.03f
    private const val TIP_WIDTH_RATIO = 0.6f

    private fun isHourIndex(index: Int): Boolean = index % 5 == 0

    fun minorTickIndices(): List<Int> = (0..59).filterNot(::isHourIndex)

    fun hourTickIndices(): List<Int> = (0..59).filter(::isHourIndex)

    private fun pointOnCircle(angleDeg: Double, radius: Float): Point {
        val angleRad = Math.toRadians(angleDeg)
        return Point(radius * sin(angleRad).toFloat(), -radius * cos(angleRad).toFloat())
    }

    fun tickLine(index: Int, r: Float): TickLine {
        val angleDeg = index * 6.0
        val innerRatio = if (isHourIndex(index)) HOUR_TICK_INNER_RATIO else MINOR_TICK_INNER_RATIO
        val outer = pointOnCircle(angleDeg, r * TICK_OUTER_RATIO)
        val inner = pointOnCircle(angleDeg, r * innerRatio)
        return TickLine(outer, inner)
    }

    private fun tickLines(indices: List<Int>, r: Float): FloatArray {
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

    fun minorTickLines(r: Float): FloatArray = tickLines(minorTickIndices(), r)

    fun hourTickLines(r: Float): FloatArray = tickLines(hourTickIndices(), r)

    fun numeralCenter(hour12: Int, r: Float): Point =
        pointOnCircle((hour12 % 12) * 30.0, r * NUMERAL_RADIUS_RATIO)

    fun handRect(kind: HandKind, r: Float): HandRect = when (kind) {
        HandKind.HOUR -> HandRect(width = 0.06f * r, tailY = 0.08f * r, tipY = -0.50f * r)
        HandKind.MINUTE -> HandRect(width = 0.05f * r, tailY = 0.08f * r, tipY = -0.74f * r)
        HandKind.SECOND -> HandRect(width = 0.012f * r, tailY = 0.18f * r, tipY = -0.80f * r)
        HandKind.ALARM -> HandRect(width = 0.02f * r, tailY = 0f, tipY = -0.60f * r)
    }

    fun tipSegment(kind: HandKind, r: Float): TipSegment {
        require(kind != HandKind.SECOND) { "the second hand has no tip color segment" }
        val rect = handRect(kind, r)
        if (kind == HandKind.ALARM) {
            return TipSegment(startY = rect.tipY, endY = rect.tipY + 0.08f * r, width = 0.05f * r)
        }
        val zoneLength = if (kind == HandKind.HOUR) 0.18f * r else 0.22f * r
        val margin = TIP_MARGIN_RATIO * r
        return TipSegment(
            startY = rect.tipY + margin,
            endY = rect.tipY + zoneLength - margin,
            width = rect.width * TIP_WIDTH_RATIO,
        )
    }

    fun centerCapRadius(r: Float): Float = CENTER_CAP_RATIO * r
}
