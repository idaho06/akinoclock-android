package org.akinosoft.akinoclock.weather.ui

import androidx.annotation.DrawableRes
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.weather.model.WeatherCondition

/** Pure condition×day/night -> drawable mapping. Night variants exist only for CLEAR and
 * PARTLY_CLOUDY; every other condition always shows its day icon. */
object WeatherIcons {

    @DrawableRes
    fun drawableRes(condition: WeatherCondition, isDay: Boolean): Int = when (condition) {
        WeatherCondition.CLEAR -> if (isDay) R.drawable.ic_weather_sunny else R.drawable.ic_weather_clear_night
        WeatherCondition.PARTLY_CLOUDY ->
            if (isDay) R.drawable.ic_weather_partly_cloudy_day else R.drawable.ic_weather_partly_cloudy_night
        WeatherCondition.OVERCAST -> R.drawable.ic_weather_cloud
        WeatherCondition.FOG -> R.drawable.ic_weather_foggy
        WeatherCondition.DRIZZLE -> R.drawable.ic_weather_rainy_light
        WeatherCondition.RAIN -> R.drawable.ic_weather_rainy
        WeatherCondition.SNOW -> R.drawable.ic_weather_weather_snowy
        WeatherCondition.THUNDERSTORM -> R.drawable.ic_weather_thunderstorm
    }
}
