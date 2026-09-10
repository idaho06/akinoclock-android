package org.akinosoft.akinoclock.clock

interface TickScheduler {
    fun start(onTick: () -> Unit)
    fun stop()
}
