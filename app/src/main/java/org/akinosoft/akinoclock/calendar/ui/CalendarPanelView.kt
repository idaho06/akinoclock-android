package org.akinosoft.akinoclock.calendar.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.model.CalendarUiState

/** Grid + today list + permission button. `MainActivity` wires the button's click in Step 6. */
class CalendarPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    val monthGridView: MonthGridView
    val todayEventsView: TodayEventsView
    val grantAccessButton: Button

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_calendar_panel, this, true)
        monthGridView = findViewById(R.id.monthGridView)
        todayEventsView = findViewById(R.id.todayEventsView)
        grantAccessButton = findViewById(R.id.grantAccessButton)
    }

    fun render(state: CalendarUiState) {
        val grid = when (state) {
            is CalendarUiState.Loading -> null
            is CalendarUiState.Granted -> state.grid
            is CalendarUiState.NotGranted -> state.grid
        }
        grid?.let(monthGridView::setGrid)

        when (state) {
            is CalendarUiState.Loading -> Unit
            is CalendarUiState.Granted -> {
                grantAccessButton.visibility = View.GONE
                todayEventsView.setEvents(state.todayList, state.today)
                monthGridView.isEnabled = true
            }
            is CalendarUiState.NotGranted -> {
                grantAccessButton.visibility = View.VISIBLE
                todayEventsView.setEvents(emptyList(), state.grid.month.atDay(1))
                monthGridView.isEnabled = false
            }
        }
    }
}
