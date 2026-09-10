package org.akinosoft.akinoclock.util

class FakePeriodicScheduler : PeriodicScheduler {

    var startCount = 0
        private set
    var stopCount = 0
        private set

    private var onTick: (() -> Unit)? = null

    val isRunning: Boolean get() = onTick != null

    override fun start(onTick: () -> Unit) {
        startCount++
        this.onTick = onTick
    }

    override fun stop() {
        stopCount++
        onTick = null
    }

    fun fireTick() {
        onTick?.invoke()
    }
}
