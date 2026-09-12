package org.akinosoft.akinoclock.weather.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun `wmo codes map to the expected condition`() {
        val cases = mapOf(
            0 to WeatherCondition.CLEAR,
            1 to WeatherCondition.PARTLY_CLOUDY,
            2 to WeatherCondition.PARTLY_CLOUDY,
            3 to WeatherCondition.OVERCAST,
            45 to WeatherCondition.FOG,
            48 to WeatherCondition.FOG,
            51 to WeatherCondition.DRIZZLE,
            53 to WeatherCondition.DRIZZLE,
            55 to WeatherCondition.DRIZZLE,
            56 to WeatherCondition.DRIZZLE,
            57 to WeatherCondition.DRIZZLE,
            61 to WeatherCondition.RAIN,
            63 to WeatherCondition.RAIN,
            65 to WeatherCondition.RAIN,
            66 to WeatherCondition.RAIN,
            67 to WeatherCondition.RAIN,
            80 to WeatherCondition.RAIN,
            81 to WeatherCondition.RAIN,
            82 to WeatherCondition.RAIN,
            71 to WeatherCondition.SNOW,
            73 to WeatherCondition.SNOW,
            75 to WeatherCondition.SNOW,
            77 to WeatherCondition.SNOW,
            85 to WeatherCondition.SNOW,
            86 to WeatherCondition.SNOW,
            95 to WeatherCondition.THUNDERSTORM,
            96 to WeatherCondition.THUNDERSTORM,
            99 to WeatherCondition.THUNDERSTORM,
        )

        cases.forEach { (code, expected) ->
            assertEquals("code $code", expected, WeatherCondition.fromWmoCode(code))
        }
    }

    @Test
    fun `unknown codes fall back to overcast rather than crashing`() {
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromWmoCode(-1))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromWmoCode(42))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromWmoCode(100))
    }
}
