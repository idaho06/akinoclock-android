package org.akinosoft.akinoclock.calendar.data

import kotlinx.coroutines.flow.Flow
import org.akinosoft.akinoclock.calendar.model.EventInstance

interface CalendarRepository {
    suspend fun instancesBetween(begin: Long, end: Long): List<EventInstance>
    fun changes(): Flow<Unit>
}
