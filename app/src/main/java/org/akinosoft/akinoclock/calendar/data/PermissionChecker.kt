package org.akinosoft.akinoclock.calendar.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

interface PermissionChecker {
    fun hasReadCalendar(): Boolean
}

class ContextPermissionChecker(private val context: Context) : PermissionChecker {

    override fun hasReadCalendar(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
}
