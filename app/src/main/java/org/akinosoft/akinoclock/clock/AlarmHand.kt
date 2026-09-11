package org.akinosoft.akinoclock.clock

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Pure angle math for the BC12 alarm hand: shown only while the alarm is strictly in the
 * future and no more than 12 hours away (a 12-hour dial cannot disambiguate further out).
 */
object AlarmHand {

    private val WINDOW: Duration = Duration.ofHours(12)

    fun angleDeg(now: Instant, alarm: Instant?, zone: ZoneId): Float? {
        if (alarm == null) return null
        val untilAlarm = Duration.between(now, alarm)
        if (untilAlarm.isNegative || untilAlarm.isZero || untilAlarm > WINDOW) return null
        val wallTime = alarm.atZone(zone)
        return ClockTime(wallTime.hour, wallTime.minute, 0).toHandAngles().hourDeg
    }
}
