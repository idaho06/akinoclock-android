package org.akinosoft.akinoclock.calendar.ui

enum class PermissionAction { REQUEST, OPEN_SETTINGS }

/**
 * Decides what the "Grant calendar access" button should do next. `shouldShowRequestPermissionRationale`
 * is false both before the permission has ever been asked and after a permanent denial — [alreadyAsked]
 * disambiguates those two cases.
 */
object PermissionButtonPolicy {
    fun decide(alreadyAsked: Boolean, canShowRationale: Boolean): PermissionAction =
        if (alreadyAsked && !canShowRationale) PermissionAction.OPEN_SETTINGS else PermissionAction.REQUEST
}
