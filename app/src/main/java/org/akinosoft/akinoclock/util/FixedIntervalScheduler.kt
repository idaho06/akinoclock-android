package org.akinosoft.akinoclock.util

import android.os.Handler

/**
 * Posts a tick every [intervalMillis], with no wall-clock alignment (unlike
 * [SecondAlignedScheduler]) — used by the RSS headline carousel's 8 s crossfade rotation.
 */
class FixedIntervalScheduler(
    private val handler: Handler,
    private val intervalMillis: Long,
) : PeriodicScheduler {

    private var onTick: (() -> Unit)? = null
    private val tickRunnable = Runnable {
        onTick?.invoke()
        scheduleNext()
    }

    override fun start(onTick: () -> Unit) {
        if (this.onTick != null) return
        this.onTick = onTick
        scheduleNext()
    }

    override fun stop() {
        onTick = null
        handler.removeCallbacks(tickRunnable)
    }

    private fun scheduleNext() {
        if (onTick == null) return
        handler.postDelayed(tickRunnable, intervalMillis)
    }
}
