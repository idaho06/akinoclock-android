package org.akinosoft.akinoclock.weather.ui

import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.weather.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconsTest {

    @Test
    fun `day icons`() {
        assertEquals(R.drawable.ic_weather_sunny, WeatherIcons.drawableRes(WeatherCondition.CLEAR, isDay = true))
        assertEquals(
            R.drawable.ic_weather_partly_cloudy_day,
            WeatherIcons.drawableRes(WeatherCondition.PARTLY_CLOUDY, isDay = true),
        )
        assertEquals(R.drawable.ic_weather_cloud, WeatherIcons.drawableRes(WeatherCondition.OVERCAST, isDay = true))
        assertEquals(R.drawable.ic_weather_foggy, WeatherIcons.drawableRes(WeatherCondition.FOG, isDay = true))
        assertEquals(R.drawable.ic_weather_rainy_light, WeatherIcons.drawableRes(WeatherCondition.DRIZZLE, isDay = true))
        assertEquals(R.drawable.ic_weather_rainy, WeatherIcons.drawableRes(WeatherCondition.RAIN, isDay = true))
        assertEquals(R.drawable.ic_weather_weather_snowy, WeatherIcons.drawableRes(WeatherCondition.SNOW, isDay = true))
        assertEquals(
            R.drawable.ic_weather_thunderstorm,
            WeatherIcons.drawableRes(WeatherCondition.THUNDERSTORM, isDay = true),
        )
    }

    @Test
    fun `night variants apply only to clear and partly cloudy`() {
        assertEquals(R.drawable.ic_weather_clear_night, WeatherIcons.drawableRes(WeatherCondition.CLEAR, isDay = false))
        assertEquals(
            R.drawable.ic_weather_partly_cloudy_night,
            WeatherIcons.drawableRes(WeatherCondition.PARTLY_CLOUDY, isDay = false),
        )
    }

    @Test
    fun `other conditions always use the day icon regardless of isDay`() {
        assertEquals(R.drawable.ic_weather_cloud, WeatherIcons.drawableRes(WeatherCondition.OVERCAST, isDay = false))
        assertEquals(R.drawable.ic_weather_foggy, WeatherIcons.drawableRes(WeatherCondition.FOG, isDay = false))
        assertEquals(R.drawable.ic_weather_rainy_light, WeatherIcons.drawableRes(WeatherCondition.DRIZZLE, isDay = false))
        assertEquals(R.drawable.ic_weather_rainy, WeatherIcons.drawableRes(WeatherCondition.RAIN, isDay = false))
        assertEquals(R.drawable.ic_weather_weather_snowy, WeatherIcons.drawableRes(WeatherCondition.SNOW, isDay = false))
        assertEquals(
            R.drawable.ic_weather_thunderstorm,
            WeatherIcons.drawableRes(WeatherCondition.THUNDERSTORM, isDay = false),
        )
    }
}
