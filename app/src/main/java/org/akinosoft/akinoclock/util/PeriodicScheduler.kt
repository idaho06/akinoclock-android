package org.akinosoft.akinoclock.util

interface PeriodicScheduler {
    fun start(onTick: () -> Unit)
    fun stop()
}
