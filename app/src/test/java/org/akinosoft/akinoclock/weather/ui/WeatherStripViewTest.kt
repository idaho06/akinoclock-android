package org.akinosoft.akinoclock.weather.ui

import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.LocalDate
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.weather.model.CurrentWeather
import org.akinosoft.akinoclock.weather.model.DayForecast
import org.akinosoft.akinoclock.weather.model.WeatherCondition
import org.akinosoft.akinoclock.weather.model.WeatherReport
import org.akinosoft.akinoclock.weather.model.WeatherUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherStripViewTest {

    private lateinit var view: WeatherStripView

    @Before
    fun setUp() {
        view = WeatherStripView(ApplicationProvider.getApplicationContext())
    }

    private fun report() = WeatherReport(
        current = CurrentWeather(temperatureC = 20.4, condition = WeatherCondition.CLEAR, isDay = true),
        days = listOf(
            DayForecast(LocalDate.parse("2026-09-12"), WeatherCondition.CLEAR, minC = 15.9, maxC = 31.5),
            DayForecast(LocalDate.parse("2026-09-13"), WeatherCondition.PARTLY_CLOUDY, minC = 17.1, maxC = 32.1),
            DayForecast(LocalDate.parse("2026-09-14"), WeatherCondition.RAIN, minC = 17.4, maxC = 33.8),
        ),
        fetchedAt = Instant.EPOCH,
    )

    @Test
    fun `Showing renders current temperature, min-max and the two forecast days`() {
        view.render(WeatherUiState.Showing(report(), stale = false))

        assertEquals("20°C", view.findViewById<TextView>(R.id.todayTemp).text.toString())
        assertEquals("16° / 32°", view.findViewById<TextView>(R.id.todayMinMax).text.toString())

        val day1 = view.findViewById<android.view.View>(R.id.forecastDay1)
        val day2 = view.findViewById<android.view.View>(R.id.forecastDay2)
        assertEquals("17° / 32°", day1.findViewById<TextView>(R.id.dayMinMax).text.toString())
        assertEquals("17° / 34°", day2.findViewById<TextView>(R.id.dayMinMax).text.toString())

        assertEquals(
            R.drawable.ic_weather_sunny,
            view.findViewById<ImageView>(R.id.todayIcon).tag,
        )
        assertEquals(
            R.drawable.ic_weather_partly_cloudy_day,
            day1.findViewById<ImageView>(R.id.dayIcon).tag,
        )
        assertEquals(
            R.drawable.ic_weather_rainy,
            day2.findViewById<ImageView>(R.id.dayIcon).tag,
        )

        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.staleGlyph).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.placeholderText).visibility)
    }

    @Test
    fun `Showing with stale true shows the stale glyph`() {
        view.render(WeatherUiState.Showing(report(), stale = true))

        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.staleGlyph).visibility)
    }

    @Test
    fun `NoLocation shows only the placeholder, and tapping it invokes onPlaceholderClick`() {
        var clicked = false
        view.onPlaceholderClick = { clicked = true }

        view.render(WeatherUiState.NoLocation)

        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.placeholderText).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.todayIcon).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.staleGlyph).visibility)

        view.findViewById<android.view.View>(R.id.placeholderText).performClick()
        assertTrue(clicked)
    }

    @Test
    fun `Loading shows nothing`() {
        view.render(WeatherUiState.Loading)

        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.todayIcon).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.placeholderText).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.staleGlyph).visibility)
    }
}
