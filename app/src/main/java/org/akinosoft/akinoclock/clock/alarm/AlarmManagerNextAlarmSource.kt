package org.akinosoft.akinoclock.clock.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AlarmManagerNextAlarmSource(private val context: Context) : NextAlarmSource {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun nextAlarm(): Instant? = alarmManager.nextAlarmClock?.triggerTime?.let(Instant::ofEpochMilli)

    override fun changes(): Flow<Instant?> = callbackFlow {
        trySend(nextAlarm())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                trySend(nextAlarm())
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
