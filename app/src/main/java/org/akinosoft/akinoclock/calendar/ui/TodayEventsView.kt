package org.akinosoft.akinoclock.calendar.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.model.EventInstance

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d")

/** Up to a handful of rows: today's events, then the next upcoming ones. */
class TodayEventsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val inflater = LayoutInflater.from(context)

    init {
        orientation = VERTICAL
    }

    fun setEvents(events: List<EventInstance>, today: LocalDate) {
        removeAllViews()
        if (events.isEmpty()) {
            addView(emptyStateView())
            return
        }
        events.forEach { addView(eventRow(it, today)) }
    }

    private fun emptyStateView(): View =
        TextView(context).apply { text = context.getString(R.string.calendar_no_events_today) }

    private fun eventRow(event: EventInstance, today: LocalDate): View {
        val row = inflater.inflate(R.layout.item_event, this, false)

        row.findViewById<TextView>(R.id.eventTime).text = if (event.allDay) {
            context.getString(R.string.calendar_all_day)
        } else {
            event.start.toLocalTime().format(TIME_FORMATTER)
        }
        row.findViewById<TextView>(R.id.eventTitle).text = event.title

        val dayView = row.findViewById<TextView>(R.id.eventDay)
        val eventDate = event.start.toLocalDate()
        if (eventDate == today) {
            dayView.visibility = View.GONE
        } else {
            dayView.visibility = View.VISIBLE
            dayView.text = eventDate.format(DAY_FORMATTER)
        }

        return row
    }
}
