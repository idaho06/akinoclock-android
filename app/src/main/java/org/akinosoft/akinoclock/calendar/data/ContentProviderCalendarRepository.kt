package org.akinosoft.akinoclock.calendar.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.model.EventInstance

private const val TAG = "CalendarRepository"

private val PROJECTION = arrayOf(
    CalendarContract.Instances.EVENT_ID,
    CalendarContract.Instances.TITLE,
    CalendarContract.Instances.BEGIN,
    CalendarContract.Instances.END,
    CalendarContract.Instances.ALL_DAY,
    CalendarContract.Instances.CALENDAR_ID,
    CalendarContract.Instances.EVENT_LOCATION,
)

private val SELECTION = "${CalendarContract.Instances.VISIBLE} = 1" +
    " AND (${CalendarContract.Instances.STATUS} IS NULL" +
    " OR ${CalendarContract.Instances.STATUS} != ${CalendarContract.Events.STATUS_CANCELED})"

class ContentProviderCalendarRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CalendarRepository {

    override suspend fun instancesBetween(begin: Long, end: Long): List<EventInstance> =
        withContext(ioDispatcher) {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, begin)
            ContentUris.appendId(builder, end)
            val uri = builder.build()

            try {
                context.contentResolver.query(uri, PROJECTION, SELECTION, null, null)?.use(::readInstances)
                    ?: emptyList()
            } catch (e: SecurityException) {
                Log.w(TAG, "READ_CALENDAR permission revoked while querying instances", e)
                emptyList()
            }
        }

    private fun readInstances(cursor: Cursor): List<EventInstance> {
        val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val systemZone = ZoneId.systemDefault()

        val instances = mutableListOf<EventInstance>()
        while (cursor.moveToNext()) {
            val allDay = cursor.getInt(allDayIndex) != 0
            val zone: ZoneId = if (allDay) ZoneOffset.UTC else systemZone
            instances += EventInstance(
                id = cursor.getLong(idIndex),
                title = cursor.getString(titleIndex) ?: context.getString(R.string.calendar_no_title),
                start = Instant.ofEpochMilli(cursor.getLong(beginIndex)).atZone(zone),
                end = Instant.ofEpochMilli(cursor.getLong(endIndex)).atZone(zone),
                allDay = allDay,
            )
        }
        return instances
    }

    override fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        // Unlike a query, registering an observer on this authority throws immediately
        // (not just returning empty) when READ_CALENDAR isn't granted.
        try {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_CALENDAR permission revoked; not observing calendar changes", e)
        }
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
}
