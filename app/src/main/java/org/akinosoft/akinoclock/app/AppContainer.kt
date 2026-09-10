package org.akinosoft.akinoclock.app

import android.content.Context
import java.time.Clock
import org.akinosoft.akinoclock.calendar.data.CalendarPrefs
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContentProviderCalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContextPermissionChecker
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.calendar.data.SharedPreferencesCalendarPrefs

class AppContainer(private val context: Context) {

    val clock: Clock = Clock.systemDefaultZone()

    val calendarRepository: CalendarRepository = ContentProviderCalendarRepository(context)
    val permissionChecker: PermissionChecker = ContextPermissionChecker(context)
    val calendarPrefs: CalendarPrefs = SharedPreferencesCalendarPrefs(context)
}
