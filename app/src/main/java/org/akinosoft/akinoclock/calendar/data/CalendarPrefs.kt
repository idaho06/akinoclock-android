package org.akinosoft.akinoclock.calendar.data

import android.content.Context

interface CalendarPrefs {
    fun permissionAsked(): Boolean
    fun setPermissionAsked()
}

class SharedPreferencesCalendarPrefs(context: Context) : CalendarPrefs {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun permissionAsked(): Boolean = prefs.getBoolean(KEY_PERMISSION_ASKED, false)

    override fun setPermissionAsked() {
        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "calendar_prefs"
        private const val KEY_PERMISSION_ASKED = "calendar_permission_asked"
    }
}
