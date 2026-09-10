package org.akinosoft.akinoclock.clock

data class HandAngles(val hourDeg: Float, val minuteDeg: Float, val secondDeg: Float)

fun ClockTime.toHandAngles(): HandAngles {
    val hour12 = hour % 12
    val hourDeg = hour12 * 30f + minute * 0.5f + second / 120f
    val minuteDeg = minute * 6f + second * 0.1f
    val secondDeg = second * 6f
    return HandAngles(hourDeg, minuteDeg, secondDeg)
}
