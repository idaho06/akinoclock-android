package org.akinosoft.akinoclock.calendar.model

import java.time.ZonedDateTime

data class EventInstance(
    val id: Long,
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val allDay: Boolean,
)
