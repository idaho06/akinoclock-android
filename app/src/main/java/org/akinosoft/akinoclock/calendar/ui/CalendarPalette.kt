package org.akinosoft.akinoclock.calendar.ui

import android.content.Context
import org.akinosoft.akinoclock.R

data class CalendarPalette(
    val today: Int,
    val accent: Int,
    val dim: Int,
    val normal: Int,
    val todayText: Int,
) {
    companion object {
        fun fromResources(context: Context): CalendarPalette = CalendarPalette(
            today = context.getColor(R.color.calendar_today),
            accent = context.getColor(R.color.calendar_accent),
            dim = context.getColor(R.color.calendar_dim),
            normal = context.getColor(R.color.numeral),
            todayText = context.getColor(R.color.calendar_today_text),
        )
    }
}
