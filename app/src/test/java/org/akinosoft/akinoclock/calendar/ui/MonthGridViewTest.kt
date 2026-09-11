package org.akinosoft.akinoclock.calendar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.YearMonth
import org.akinosoft.akinoclock.calendar.logic.MonthGridBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonthGridViewTest {

    private val month = YearMonth.of(2026, 9)
    private val today = LocalDate.of(2026, 9, 25)
    private val eventDays = setOf(LocalDate.of(2026, 9, 26))
    private val grid = MonthGridBuilder.build(month, today, eventDays)

    private fun colorDistance(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return dr * dr + dg * dg + db * db
    }

    private fun closestNeighborhoodMatch(bitmap: Bitmap, centerX: Int, centerY: Int, target: Int): Int {
        val neighborhood = (centerX - 5..centerX + 5).flatMap { x ->
            (centerY - 5..centerY + 5).map { y -> bitmap.getPixel(x, y) }
        }
        return neighborhood.minByOrNull { colorDistance(it, target) }!!
    }

    private fun drawnBitmap(view: MonthGridView, width: Int = 350, height: Int = 300): Bitmap {
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    @Test
    fun `today's cell is drawn with the highlight color at its center`() {
        val view = MonthGridView(ApplicationProvider.getApplicationContext())
        view.setGrid(grid)
        val bitmap = drawnBitmap(view)

        val todayIndex = grid.cells.indexOfFirst { it.date == today }
        val rect = view.cellRect(todayIndex)

        val closest = closestNeighborhoodMatch(bitmap, rect.centerX().toInt(), rect.centerY().toInt(), view.palette.today)
        assertTrue(colorDistance(closest, view.palette.today) < colorDistance(closest, view.palette.dim))
    }

    @Test
    fun `today's number is drawn with the todayText color, not normal`() {
        val view = MonthGridView(ApplicationProvider.getApplicationContext())
        view.palette = view.palette.copy(todayText = Color.MAGENTA, normal = Color.BLUE)
        view.setGrid(grid)
        val bitmap = drawnBitmap(view)

        val todayIndex = grid.cells.indexOfFirst { it.date == today }
        val rect = view.cellRect(todayIndex)

        val closest = closestNeighborhoodMatch(bitmap, rect.centerX().toInt(), rect.centerY().toInt(), Color.MAGENTA)
        assertTrue(colorDistance(closest, Color.MAGENTA) < colorDistance(closest, Color.BLUE))
    }

    @Test
    fun `a cell with events has the accent color at its dot position`() {
        val view = MonthGridView(ApplicationProvider.getApplicationContext())
        view.setGrid(grid)
        val bitmap = drawnBitmap(view)

        val eventIndex = grid.cells.indexOfFirst { it.hasEvents }
        val rect = view.cellRect(eventIndex)
        val dotX = rect.centerX().toInt()
        val dotY = (rect.bottom - rect.height() * 0.12f).toInt()

        assertEquals(view.palette.accent, bitmap.getPixel(dotX, dotY))
    }

    @Test
    fun `a not-in-month cell's number is drawn with the dimmed color`() {
        val view = MonthGridView(ApplicationProvider.getApplicationContext())
        view.setGrid(grid)
        val bitmap = drawnBitmap(view)

        val outsideIndex = grid.cells.indexOfFirst { !it.inCurrentMonth }
        val rect = view.cellRect(outsideIndex)

        val closest = closestNeighborhoodMatch(bitmap, rect.centerX().toInt(), rect.centerY().toInt(), view.palette.dim)
        assertTrue(colorDistance(closest, view.palette.dim) < colorDistance(closest, view.palette.normal))
    }

    @Test
    fun `the weekday header row draws a label in every column`() {
        val view = MonthGridView(ApplicationProvider.getApplicationContext())
        view.setGrid(grid)
        val bitmap = drawnBitmap(view)

        for (col in 0..6) {
            val rect = view.headerRect(col)
            val closest =
                closestNeighborhoodMatch(bitmap, rect.centerX().toInt(), rect.centerY().toInt(), view.palette.dim)
            assertTrue(
                "column $col has no header label",
                colorDistance(closest, view.palette.dim) < colorDistance(closest, Color.BLACK),
            )
        }
    }

    @Test
    fun `cells stretch independently to fill the full width and height of the view`() {
        val view = MonthGridView(ApplicationProvider.getApplicationContext())
        view.layout(0, 0, 700, 210)

        // cellWidth = 700/7 = 100, cellHeight = 210/7 = 30 (independent, no squaring).
        val header = view.headerRect(0)
        assertEquals(0f, header.left, 0.01f)
        assertEquals(0f, header.top, 0.01f)
        assertEquals(100f, header.width(), 0.01f)
        assertEquals(30f, header.height(), 0.01f)

        val lastCell = view.cellRect(41) // row 6 (last grid row), col 6 (last column)
        assertEquals(700f, lastCell.right, 0.01f)
        assertEquals(210f, lastCell.bottom, 0.01f)
    }

    @Test
    fun `setGrid triggers invalidate`() {
        var invalidateCount = 0
        val trackingView = object : MonthGridView(ApplicationProvider.getApplicationContext()) {
            override fun invalidate() {
                invalidateCount++
                super.invalidate()
            }
        }

        trackingView.setGrid(grid)

        assertEquals(1, invalidateCount)
    }
}
