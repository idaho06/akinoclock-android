package org.akinosoft.akinoclock.util

import android.os.Handler

/**
 * Posts a tick once per second, re-aligning to the wall-clock second boundary on every repost
 * so scheduling jitter never accumulates into drift. Used by `ClockView`'s 1 Hz second hand.
 */
class SecondAlignedScheduler(
    private val handler: Handler,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
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
        handler.postDelayed(tickRunnable, delayToNextSecondMillis())
    }

    internal fun delayToNextSecondMillis(): Long = 1000L - (nowMillis() % 1000L)
}
