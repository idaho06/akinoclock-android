package org.akinosoft.akinoclock.clock

import android.content.Context
import org.akinosoft.akinoclock.R

data class DialPalette(
    val dialBackground: Int,
    val bezelRing: Int,
    val numeral: Int,
    val tickMinute: Int,
    val handBaton: Int,
    val handTip: Int,
    val secondHand: Int,
    val centerCapRing: Int,
    val alarmHand: Int,
    val alarmTip: Int,
) {
    companion object {
        fun fromResources(context: Context): DialPalette = DialPalette(
            dialBackground = context.getColor(R.color.dial_background),
            bezelRing = context.getColor(R.color.bezel_ring),
            numeral = context.getColor(R.color.numeral),
            tickMinute = context.getColor(R.color.tick_minute),
            handBaton = context.getColor(R.color.hand_baton),
            handTip = context.getColor(R.color.hand_tip),
            secondHand = context.getColor(R.color.second_hand),
            centerCapRing = context.getColor(R.color.center_cap_ring),
            alarmHand = context.getColor(R.color.alarm_hand),
            alarmTip = context.getColor(R.color.alarm_tip),
        )
    }
}
