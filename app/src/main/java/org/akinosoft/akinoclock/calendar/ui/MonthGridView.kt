package org.akinosoft.akinoclock.calendar.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import org.akinosoft.akinoclock.calendar.model.MonthGrid

private const val GRID_COLUMNS = 7
private const val GRID_ROWS = 6
private const val TOTAL_ROWS = GRID_ROWS + 1 // + the weekday header row

/**
 * Month grid on a Canvas: a weekday header row plus a 6x7 grid of day cells, each with an
 * optional today-highlight and an event dot. Text sizes are relative to cell size, not `sp`,
 * so the device's 1.3 font scale cannot break the layout.
 */
open class MonthGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var palette: CalendarPalette = CalendarPalette.fromResources(context)
        set(value) {
            field = value
            invalidate()
        }

    private var grid: MonthGrid? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setGrid(newGrid: MonthGrid) {
        grid = newGrid
        invalidate()
    }

    /** The rect for grid cell [index] (0..41), below the weekday header row. */
    fun cellRect(index: Int): RectF {
        val row = index / GRID_COLUMNS + 1
        val col = index % GRID_COLUMNS
        return columnRect(col, row)
    }

    /** The rect for weekday header column [col] (0 = Monday .. 6 = Sunday). */
    fun headerRect(col: Int): RectF = columnRect(col, row = 0)

    private var geometryWidth = -1
    private var geometryHeight = -1
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private fun ensureGeometry() {
        if (geometryWidth == width && geometryHeight == height) return
        geometryWidth = width
        geometryHeight = height
        cellSize = minOf(width.toFloat() / GRID_COLUMNS, height.toFloat() / TOTAL_ROWS)
        offsetX = (width - cellSize * GRID_COLUMNS) / 2f
        offsetY = (height - cellSize * TOTAL_ROWS) / 2f
    }

    private fun columnRect(col: Int, row: Int): RectF {
        ensureGeometry()
        val left = offsetX + col * cellSize
        val top = offsetY + row * cellSize
        return RectF(left, top, left + cellSize, top + cellSize)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, rect: RectF, color: Int, textSizeRatio: Float) {
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = rect.height() * textSizeRatio
        val textY = rect.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, rect.centerX(), textY, paint)
    }

    override fun onDraw(canvas: Canvas) {
        val currentGrid = grid ?: return

        for (col in 0 until GRID_COLUMNS) {
            val label = DayOfWeek.MONDAY.plus(col.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            drawCenteredText(canvas, label, headerRect(col), palette.dim, textSizeRatio = 0.3f)
        }

        currentGrid.cells.forEachIndexed { index, cell ->
            val rect = cellRect(index)

            if (cell.isToday) {
                paint.color = palette.today
                val radius = minOf(rect.width(), rect.height()) * 0.35f
                canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint)
            }

            val textColor = when {
                cell.isToday -> palette.todayText
                cell.inCurrentMonth -> palette.normal
                else -> palette.dim
            }
            drawCenteredText(canvas, cell.date.dayOfMonth.toString(), rect, textColor, textSizeRatio = 0.35f)

            if (cell.hasEvents) {
                paint.color = palette.accent
                val dotRadius = rect.height() * 0.05f
                canvas.drawCircle(rect.centerX(), rect.bottom - rect.height() * 0.12f, dotRadius, paint)
            }
        }
    }
}
