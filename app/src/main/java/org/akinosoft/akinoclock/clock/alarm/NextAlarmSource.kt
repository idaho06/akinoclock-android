package org.akinosoft.akinoclock.clock.alarm

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface NextAlarmSource {
    fun nextAlarm(): Instant?
    fun changes(): Flow<Instant?>
}
