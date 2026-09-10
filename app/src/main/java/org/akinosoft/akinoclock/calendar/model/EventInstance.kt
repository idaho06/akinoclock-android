package org.akinosoft.akinoclock.calendar.model

import java.time.ZonedDateTime

/**
 * [start] and [end] are always pre-zoned correctly by whoever constructs this: UTC for
 * all-day events, the system default zone otherwise. Consumers should read their zone
 * as-is rather than re-deriving it from [allDay].
 */
data class EventInstance(
    val id: Long,
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val allDay: Boolean,
)
