package org.akinosoft.akinoclock.calendar.ui

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.logic.MonthGridBuilder
import org.akinosoft.akinoclock.calendar.model.CalendarUiState
import org.akinosoft.akinoclock.calendar.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarPanelViewTest {

    private val month = YearMonth.of(2026, 9)
    private val today = LocalDate.of(2026, 9, 25)

    private fun panel(): CalendarPanelView = CalendarPanelView(ApplicationProvider.getApplicationContext())

    @Test
    fun `Granted hides the button and shows the grid and today list`() {
        val grid = MonthGridBuilder.build(month, today, setOf(LocalDate.of(2026, 9, 26)))
        val events = listOf(
            EventInstance(
                id = 1, title = "Standup",
                start = ZonedDateTime.of(today, java.time.LocalTime.of(9, 0), ZoneOffset.UTC),
                end = ZonedDateTime.of(today, java.time.LocalTime.of(9, 30), ZoneOffset.UTC),
                allDay = false,
            ),
        )
        val view = panel()

        view.render(CalendarUiState.Granted(grid, events, today))

        assertEquals(View.GONE, view.grantAccessButton.visibility)
        assertEquals(1, view.todayEventsView.childCount)
    }

    @Test
    fun `NotGranted shows the button and the grid without dots, and an empty today list`() {
        val grid = MonthGridBuilder.build(month, today, emptySet())
        val view = panel()

        view.render(CalendarUiState.NotGranted(grid))

        assertEquals(View.VISIBLE, view.grantAccessButton.visibility)
        assertTrue(grid.cells.none { it.hasEvents })
        assertEquals(1, view.todayEventsView.childCount)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(
            context.getString(R.string.calendar_no_events_today),
            (view.todayEventsView.getChildAt(0) as TextView).text,
        )
    }

    @Test
    fun `Loading leaves previously rendered content in place`() {
        val grid = MonthGridBuilder.build(month, today, setOf(LocalDate.of(2026, 9, 26)))
        val events = listOf(
            EventInstance(
                id = 1, title = "Standup",
                start = ZonedDateTime.of(today, java.time.LocalTime.of(9, 0), ZoneOffset.UTC),
                end = ZonedDateTime.of(today, java.time.LocalTime.of(9, 30), ZoneOffset.UTC),
                allDay = false,
            ),
        )
        val view = panel()
        view.render(CalendarUiState.Granted(grid, events, today))

        view.render(CalendarUiState.Loading)

        assertEquals(View.GONE, view.grantAccessButton.visibility)
        assertEquals(1, view.todayEventsView.childCount)
    }
}
