package org.akinosoft.akinoclock.clock

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmHandTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    @Test
    fun `null alarm has no angle`() {
        val now = Instant.parse("2026-09-12T10:00:00Z")
        assertNull(AlarmHand.angleDeg(now, null, zone))
    }

    @Test
    fun `alarm 1 minute in the past has no angle`() {
        val now = Instant.parse("2026-09-12T10:00:00Z")
        val alarm = now.minusSeconds(60)
        assertNull(AlarmHand.angleDeg(now, alarm, zone))
    }

    @Test
    fun `alarm equal to now has no angle`() {
        val now = Instant.parse("2026-09-12T10:00:00Z")
        assertNull(AlarmHand.angleDeg(now, now, zone))
    }

    @Test
    fun `alarm 1 second in the future has an angle`() {
        val now = Instant.parse("2026-09-12T10:00:00Z")
        val alarm = now.plusSeconds(1)
        assertEquals(expectedAngle(alarm), AlarmHand.angleDeg(now, alarm, zone)!!, 0.01f)
    }

    @Test
    fun `alarm exactly 12 hours away has an angle (boundary inclusive)`() {
        val now = Instant.parse("2026-09-12T10:00:00Z")
        val alarm = now.plusSeconds(12 * 3600L)
        assertEquals(expectedAngle(alarm), AlarmHand.angleDeg(now, alarm, zone)!!, 0.01f)
    }

    @Test
    fun `alarm 12 hours and 1 second away has no angle`() {
        val now = Instant.parse("2026-09-12T10:00:00Z")
        val alarm = now.plusSeconds(12 * 3600L + 1)
        assertNull(AlarmHand.angleDeg(now, alarm, zone))
    }

    @Test
    fun `alarm at 06-30 local resolves to 195 degrees`() {
        val now = Instant.parse("2026-09-12T00:00:00Z")
        val alarm = Instant.parse("2026-09-12T04:30:00Z") // 06:30 Europe/Madrid (CEST, UTC+2)
        assertEquals(195f, AlarmHand.angleDeg(now, alarm, zone)!!, 0.01f)
    }

    @Test
    fun `alarm at 18-30 local also resolves to 195 degrees (12-hour dial)`() {
        val now = Instant.parse("2026-09-12T08:00:00Z")
        val alarm = Instant.parse("2026-09-12T16:30:00Z") // 18:30 Europe/Madrid (CEST, UTC+2)
        assertEquals(195f, AlarmHand.angleDeg(now, alarm, zone)!!, 0.01f)
    }

    @Test
    fun `alarm at midnight local resolves to 0 degrees`() {
        val now = Instant.parse("2026-09-12T22:00:00Z") // 2026-09-13T00:00 CEST minus a moment
        val alarm = Instant.parse("2026-09-12T22:00:01Z") // 2026-09-13T00:00:01 CEST
        assertEquals(0f, AlarmHand.angleDeg(now, alarm, zone)!!, 0.01f)
    }

    @Test
    fun `alarm across the October DST change resolves to the wall time on the alarm's day`() {
        // Europe/Madrid switches from CEST (UTC+2) to CET (UTC+1) at 2026-10-25T01:00:00Z.
        // "now" is before the change (CEST); the alarm is after it (CET), at 06:30 local wall time.
        val now = Instant.parse("2026-10-24T20:00:00Z") // 2026-10-24T22:00 CEST
        val alarm = Instant.parse("2026-10-25T05:30:00Z") // 2026-10-25T06:30 CET
        assertEquals(195f, AlarmHand.angleDeg(now, alarm, zone)!!, 0.01f)
    }

    private fun expectedAngle(alarm: Instant): Float {
        val wall = alarm.atZone(zone)
        return ClockTime(wall.hour, wall.minute, 0).toHandAngles().hourDeg
    }
}
