package org.akinosoft.akinoclock.weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.weather.model.DayForecast
import org.akinosoft.akinoclock.weather.model.WeatherReport
import org.akinosoft.akinoclock.weather.model.WeatherUiState

/**
 * A fixed-height strip showing today's condition/temperature/min-max plus a two-day forecast.
 * [render] toggles between the three [WeatherUiState] variants; `NoLocation` shows only the
 * placeholder (tap fires [onPlaceholderClick], wired by `MainActivity` to open Settings).
 */
class WeatherStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    var onPlaceholderClick: (() -> Unit)? = null

    private val todayIcon: ImageView
    private val todayColumn: View
    private val todayTemp: TextView
    private val todayMinMax: TextView
    private val forecastDay1: View
    private val forecastDay2: View
    private val staleGlyph: View
    private val placeholderText: View

    private val content: List<View>

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_weather_strip, this, true)

        todayIcon = findViewById(R.id.todayIcon)
        todayColumn = findViewById(R.id.todayColumn)
        todayTemp = findViewById(R.id.todayTemp)
        todayMinMax = findViewById(R.id.todayMinMax)
        forecastDay1 = findViewById(R.id.forecastDay1)
        forecastDay2 = findViewById(R.id.forecastDay2)
        staleGlyph = findViewById(R.id.staleGlyph)
        placeholderText = findViewById(R.id.placeholderText)

        content = listOf(todayIcon, todayColumn, forecastDay1, forecastDay2, staleGlyph)
        placeholderText.setOnClickListener { onPlaceholderClick?.invoke() }
    }

    fun render(state: WeatherUiState) {
        when (state) {
            WeatherUiState.NoLocation -> showOnly(placeholderText)
            WeatherUiState.Loading -> showOnly()
            is WeatherUiState.Showing -> {
                showOnly(*content.toTypedArray())
                staleGlyph.visibility = if (state.stale) VISIBLE else GONE
                bind(state.report)
            }
        }
    }

    private fun showOnly(vararg visible: View) {
        placeholderText.visibility = if (placeholderText in visible) VISIBLE else GONE
        content.forEach { it.visibility = if (it in visible) VISIBLE else GONE }
    }

    private fun bind(report: WeatherReport) {
        todayIcon.setIcon(report.current.condition, report.current.isDay)
        todayTemp.text = context.getString(R.string.weather_current_format, Math.round(report.current.temperatureC).toInt())
        val today = report.days[0]
        todayMinMax.text = minMaxText(today)

        bindDay(forecastDay1, report.days[1])
        bindDay(forecastDay2, report.days[2])
    }

    private fun bindDay(container: View, day: DayForecast) {
        container.findViewById<TextView>(R.id.dayWeekday).text = WEEKDAY_FORMAT.format(day.date)
        container.findViewById<ImageView>(R.id.dayIcon).setIcon(day.condition, isDay = true)
        container.findViewById<TextView>(R.id.dayMinMax).text = minMaxText(day)
    }

    private fun minMaxText(day: DayForecast): String =
        context.getString(R.string.weather_min_max_format, Math.round(day.minC).toInt(), Math.round(day.maxC).toInt())

    private fun ImageView.setIcon(condition: org.akinosoft.akinoclock.weather.model.WeatherCondition, isDay: Boolean) {
        val res = WeatherIcons.drawableRes(condition, isDay)
        if (tag != res) {
            setImageResource(res)
            tag = res
        }
    }

    private companion object {
        val WEEKDAY_FORMAT: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault())
    }
}
