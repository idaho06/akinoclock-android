package org.akinosoft.akinoclock.calendar.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.time.DayOfWeek
import java.time.LocalDate
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

    /** Fires with the tapped cell's date; never fires for a tap on the weekday header row, or
     * while [isEnabled] is false (e.g. calendar permission not granted — see CalendarPanelView). */
    var onDateClick: ((LocalDate) -> Unit)? = null

    private var grid: MonthGrid? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
    }

    fun setGrid(newGrid: MonthGrid) {
        grid = newGrid
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled && event.action == MotionEvent.ACTION_UP) {
            dateAt(event.x, event.y)?.let { onDateClick?.invoke(it) }
        }
        return true
    }

    /** The date at ([x], [y]), or `null` for the weekday header row or out-of-grid coordinates. */
    private fun dateAt(x: Float, y: Float): LocalDate? {
        val currentGrid = grid ?: return null
        val col = (x / cellWidth()).toInt().coerceIn(0, GRID_COLUMNS - 1)
        val row = (y / cellHeight()).toInt()
        if (row !in 1 until TOTAL_ROWS) return null
        val index = (row - 1) * GRID_COLUMNS + col
        return currentGrid.cells.getOrNull(index)?.date
    }

    /** The rect for grid cell [index] (0..41), below the weekday header row. */
    fun cellRect(index: Int): RectF {
        val row = index / GRID_COLUMNS + 1
        val col = index % GRID_COLUMNS
        return columnRect(col, row)
    }

    /** The rect for weekday header column [col] (0 = Monday .. 6 = Sunday). */
    fun headerRect(col: Int): RectF = columnRect(col, row = 0)

    private fun columnRect(col: Int, row: Int): RectF {
        val cellWidth = cellWidth()
        val cellHeight = cellHeight()
        val left = col * cellWidth
        val top = row * cellHeight
        return RectF(left, top, left + cellWidth, top + cellHeight)
    }

    private fun cellWidth() = width.toFloat() / GRID_COLUMNS

    private fun cellHeight() = height.toFloat() / TOTAL_ROWS

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
            drawCenteredText(canvas, label, headerRect(col), palette.dim, textSizeRatio = 0.44f)
        }

        currentGrid.cells.forEachIndexed { index, cell ->
            val rect = cellRect(index)

            if (cell.isToday) {
                paint.color = palette.today
                val radius = minOf(rect.width(), rect.height()) * 0.40f
                canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint)
            }

            val textColor = when {
                cell.isToday -> palette.todayText
                cell.inCurrentMonth -> palette.normal
                else -> palette.dim
            }
            drawCenteredText(canvas, cell.date.dayOfMonth.toString(), rect, textColor, textSizeRatio = 0.54f)

            if (cell.hasEvents) {
                paint.color = palette.accent
                val dotRadius = rect.height() * 0.05f
                canvas.drawCircle(rect.centerX(), rect.bottom - rect.height() * 0.12f, dotRadius, paint)
            }
        }
    }
}
