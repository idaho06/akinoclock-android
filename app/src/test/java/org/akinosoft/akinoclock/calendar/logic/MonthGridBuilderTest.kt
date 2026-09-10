package org.akinosoft.akinoclock.calendar.logic

import java.time.LocalDate
import java.time.YearMonth
import org.akinosoft.akinoclock.calendar.model.DayCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthGridBuilderTest {

    @Test
    fun `September 2026 grid has 42 cells starting the Monday before the 1st`() {
        val grid = MonthGridBuilder.build(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 1),
            eventDays = emptySet(),
        )

        assertEquals(42, grid.cells.size)
        assertEquals(LocalDate.of(2026, 8, 31), grid.cells[0].date)
        assertFalse(grid.cells[0].inCurrentMonth)
        assertEquals(LocalDate.of(2026, 9, 1), grid.cells[1].date)
        assertTrue(grid.cells[1].inCurrentMonth)
        assertEquals(LocalDate.of(2026, 9, 30), grid.cells[30].date)
        assertTrue(grid.cells[30].inCurrentMonth)
        assertEquals(LocalDate.of(2026, 10, 11), grid.cells[41].date)
        assertFalse(grid.cells[41].inCurrentMonth)
    }

    @Test
    fun `February 2027 starts on a Monday and still yields 42 cells with two trailing March rows`() {
        val grid = MonthGridBuilder.build(
            month = YearMonth.of(2027, 2),
            today = LocalDate.of(2027, 2, 1),
            eventDays = emptySet(),
        )

        assertEquals(42, grid.cells.size)
        assertEquals(LocalDate.of(2027, 2, 1), grid.cells[0].date)
        assertTrue(grid.cells[0].inCurrentMonth)
        assertEquals(LocalDate.of(2027, 2, 28), grid.cells[27].date)
        assertTrue(grid.cells[27].inCurrentMonth)
        assertEquals(LocalDate.of(2027, 3, 1), grid.cells[28].date)
        assertFalse(grid.cells[28].inCurrentMonth)
        assertEquals(LocalDate.of(2027, 3, 14), grid.cells[41].date)
        assertFalse(grid.cells[41].inCurrentMonth)
    }

    @Test
    fun `isToday is set only for the cell matching today, when today falls in the grid`() {
        val grid = MonthGridBuilder.build(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 25),
            eventDays = emptySet(),
        )

        val todayCells = grid.cells.filter(DayCell::isToday)
        assertEquals(1, todayCells.size)
        assertEquals(LocalDate.of(2026, 9, 25), todayCells.single().date)
    }

    @Test
    fun `today outside the month flags no cell`() {
        val grid = MonthGridBuilder.build(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 11, 1),
            eventDays = emptySet(),
        )

        assertTrue(grid.cells.none(DayCell::isToday))
    }

    @Test
    fun `hasEvents is true only for in-month dates present in eventDays`() {
        val grid = MonthGridBuilder.build(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 1),
            eventDays = setOf(
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 26),
                // trailing-month date deliberately in eventDays but must not be flagged
                LocalDate.of(2026, 8, 31),
            ),
        )

        val eventCells = grid.cells.filter(DayCell::hasEvents)
        assertEquals(2, eventCells.size)
        assertEquals(
            setOf(LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 26)),
            eventCells.map(DayCell::date).toSet(),
        )
        assertTrue(grid.cells.first { it.date == LocalDate.of(2026, 8, 31) }.hasEvents.not())
    }
}
