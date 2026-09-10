package org.akinosoft.akinoclock.calendar.ui

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodayEventsViewTest {

    private val today = LocalDate.of(2026, 9, 25)

    private fun view(): TodayEventsView = TodayEventsView(ApplicationProvider.getApplicationContext())

    private fun row(view: TodayEventsView, index: Int) = view.getChildAt(index)

    @Test
    fun `setEvents creates one row per event`() {
        val events = listOf(
            EventInstance(
                id = 1, title = "Standup",
                start = ZonedDateTime.of(today, java.time.LocalTime.of(9, 0), ZoneOffset.UTC),
                end = ZonedDateTime.of(today, java.time.LocalTime.of(9, 30), ZoneOffset.UTC),
                allDay = false,
            ),
            EventInstance(
                id = 2, title = "Cumpleaños Ester",
                start = ZonedDateTime.of(today.plusDays(1), java.time.LocalTime.MIDNIGHT, ZoneOffset.UTC),
                end = ZonedDateTime.of(today.plusDays(2), java.time.LocalTime.MIDNIGHT, ZoneOffset.UTC),
                allDay = true,
            ),
        )
        val v = view()

        v.setEvents(events, today)

        assertEquals(2, v.childCount)
    }

    @Test
    fun `a timed event today shows HH mm and no day label`() {
        val event = EventInstance(
            id = 1, title = "Standup",
            start = ZonedDateTime.of(today, java.time.LocalTime.of(9, 30), ZoneOffset.UTC),
            end = ZonedDateTime.of(today, java.time.LocalTime.of(10, 0), ZoneOffset.UTC),
            allDay = false,
        )
        val v = view()

        v.setEvents(listOf(event), today)

        val r = row(v, 0)
        assertEquals("09:30", r.findViewById<TextView>(R.id.eventTime).text)
        assertEquals("Standup", r.findViewById<TextView>(R.id.eventTitle).text)
        assertEquals(View.GONE, r.findViewById<TextView>(R.id.eventDay).visibility)
    }

    @Test
    fun `an all-day event shows the all-day text`() {
        val event = EventInstance(
            id = 1, title = "Cumpleaños Ester",
            start = ZonedDateTime.of(today, java.time.LocalTime.MIDNIGHT, ZoneOffset.UTC),
            end = ZonedDateTime.of(today.plusDays(1), java.time.LocalTime.MIDNIGHT, ZoneOffset.UTC),
            allDay = true,
        )
        val v = view()

        v.setEvents(listOf(event), today)

        val r = row(v, 0)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(context.getString(R.string.calendar_all_day), r.findViewById<TextView>(R.id.eventTime).text)
    }

    @Test
    fun `a non-today event shows a day label`() {
        val tomorrow = today.plusDays(1)
        val event = EventInstance(
            id = 1, title = "Tomorrow's meeting",
            start = ZonedDateTime.of(tomorrow, java.time.LocalTime.of(8, 0), ZoneOffset.UTC),
            end = ZonedDateTime.of(tomorrow, java.time.LocalTime.of(8, 30), ZoneOffset.UTC),
            allDay = false,
        )
        val v = view()

        v.setEvents(listOf(event), today)

        val r = row(v, 0)
        val dayView = r.findViewById<TextView>(R.id.eventDay)
        assertEquals(View.VISIBLE, dayView.visibility)
        assertTrue(dayView.text.isNotEmpty())
    }

    @Test
    fun `an empty list shows the empty-state text view`() {
        val v = view()

        v.setEvents(emptyList(), today)

        assertEquals(1, v.childCount)
        val emptyStateView = row(v, 0) as TextView
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(context.getString(R.string.calendar_no_events_today), emptyStateView.text)
    }
}
