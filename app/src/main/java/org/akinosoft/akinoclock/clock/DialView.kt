package org.akinosoft.akinoclock.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet

/**
 * The static part of the Braun BC12-style dial: bezel, minute/hour ticks and numerals, on a
 * transparent background so the window background shows through. It redraws only when its size
 * or [palette] changes, so the hardware renderer keeps reusing its recorded drawing while the
 * hands layer on top redraws every second.
 */
open class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SquareDialBase(context, attrs) {

    var palette: DialPalette = DialPalette.fromResources(context)
        set(value) {
            field = value
            invalidate()
        }

    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val minorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val numeralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private var minorTickLines = FloatArray(0)
    private var hourTickLines = FloatArray(0)
    private val numeralX = FloatArray(12)
    private val numeralBaselineY = FloatArray(12)

    /** Recomputes the geometry for the new size; the framework already invalidates on resize. */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bezelPaint.strokeWidth = 0.02f * radius
        minorTickPaint.strokeWidth = 0.012f * radius
        hourTickPaint.strokeWidth = 0.02f * radius
        minorTickLines = toAbsolute(ClockGeometry.minorTickLines(radius))
        hourTickLines = toAbsolute(ClockGeometry.hourTickLines(radius))

        numeralPaint.textSize = 0.16f * radius
        val fontMetrics = numeralPaint.fontMetrics
        val textBaselineOffset = (fontMetrics.ascent + fontMetrics.descent) / 2
        for (hour in 1..12) {
            val p = ClockGeometry.numeralCenter(hour, radius)
            numeralX[hour - 1] = centerX + p.x
            numeralBaselineY[hour - 1] = centerY + p.y - textBaselineOffset
        }
    }

    private fun toAbsolute(lines: FloatArray): FloatArray {
        val absolute = FloatArray(lines.size)
        for (i in lines.indices step 2) {
            absolute[i] = lines[i] + centerX
            absolute[i + 1] = lines[i + 1] + centerY
        }
        return absolute
    }

    override fun onDraw(canvas: Canvas) {
        bezelPaint.color = palette.bezelRing
        canvas.drawCircle(centerX, centerY, radius, bezelPaint)

        minorTickPaint.color = palette.tickMinute
        canvas.drawLines(minorTickLines, minorTickPaint)

        hourTickPaint.color = palette.numeral
        canvas.drawLines(hourTickLines, hourTickPaint)

        numeralPaint.color = palette.numeral
        for (i in 0 until 12) {
            canvas.drawText(NUMERALS[i], numeralX[i], numeralBaselineY[i], numeralPaint)
        }
    }

    private companion object {
        val NUMERALS = Array(12) { (it + 1).toString() }
    }
}
