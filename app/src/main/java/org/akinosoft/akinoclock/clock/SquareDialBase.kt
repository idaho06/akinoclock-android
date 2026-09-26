package org.akinosoft.akinoclock.clock

import android.content.Context
import android.util.AttributeSet
import android.view.View
import org.akinosoft.akinoclock.R

/**
 * Shared sizing for the dial and the hands drawn over it: measures as the largest square that
 * fits (or passes an exact size through), and keeps [radius] and the center in sync with the
 * view's size so both layers line up.
 */
abstract class SquareDialBase(
    context: Context,
    attrs: AttributeSet?,
) : View(context, attrs) {

    protected var radius = 0f
        private set
    protected var centerX = 0f
        private set
    protected var centerY = 0f
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize)
            return
        }

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            val defaultSize = resources.getDimensionPixelSize(R.dimen.clock_default_size)
            setMeasuredDimension(defaultSize, defaultSize)
            return
        }

        val boundedWidth = if (widthMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else widthSize
        val boundedHeight = if (heightMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else heightSize
        val square = minOf(boundedWidth, boundedHeight)
        setMeasuredDimension(square, square)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = minOf(w, h) / 2f
        centerX = w / 2f
        centerY = h / 2f
    }
}
