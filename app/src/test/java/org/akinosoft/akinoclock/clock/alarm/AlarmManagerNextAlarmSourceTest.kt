package org.akinosoft.akinoclock.clock.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AlarmManagerNextAlarmSourceTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun schedule(context: Context, triggerTime: Long) {
        val pendingIntent = PendingIntent.getActivity(context, 0, Intent("show-alarm"), PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pendingIntent), pendingIntent)
    }

    @Test
    fun `nextAlarm is null when nothing is scheduled`() {
        assertNull(AlarmManagerNextAlarmSource(context()).nextAlarm())
    }

    @Test
    fun `nextAlarm reflects a scheduled alarm clock`() {
        val context = context()
        val triggerTime = 1_700_000_000_000L
        schedule(context, triggerTime)

        assertEquals(Instant.ofEpochMilli(triggerTime), AlarmManagerNextAlarmSource(context).nextAlarm())
    }

    @Test
    fun `changes emits the current value immediately on collection`() = runTest(UnconfinedTestDispatcher()) {
        val context = context()
        val triggerTime = 1_700_000_000_000L
        schedule(context, triggerTime)
        val emitted = mutableListOf<Instant?>()

        val job = launch { AlarmManagerNextAlarmSource(context).changes().collect { emitted.add(it) } }

        assertEquals(listOf(Instant.ofEpochMilli(triggerTime)), emitted)
        job.cancel()
    }

    @Test
    fun `changes emits a new value when the system broadcasts a change`() = runTest(UnconfinedTestDispatcher()) {
        val context = context()
        val firstTrigger = 1_700_000_000_000L
        schedule(context, firstTrigger)
        val emitted = mutableListOf<Instant?>()
        val job = launch { AlarmManagerNextAlarmSource(context).changes().collect { emitted.add(it) } }

        val earlierTrigger = 1_600_000_000_000L
        schedule(context, earlierTrigger)
        context.sendBroadcast(Intent(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(Instant.ofEpochMilli(firstTrigger), Instant.ofEpochMilli(earlierTrigger)), emitted)
        job.cancel()
    }

    @Test
    fun `cancelling the collector unregisters the receiver`() = runTest(UnconfinedTestDispatcher()) {
        val context = context()
        val job = launch { AlarmManagerNextAlarmSource(context).changes().collect { } }

        val registeredWhileActive = shadowOf(context as Application).registeredReceivers
            .any { it.intentFilter.hasAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) }
        assertTrue(registeredWhileActive)

        job.cancel()
        job.join()

        val registeredAfterCancel = shadowOf(context).registeredReceivers
            .any { it.intentFilter.hasAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) }
        assertFalse(registeredAfterCancel)
    }
}
