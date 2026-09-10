package org.akinosoft.akinoclock.calendar.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarPrefsTest {

    @Test
    fun `permissionAsked defaults to false`() {
        val prefs = SharedPreferencesCalendarPrefs(ApplicationProvider.getApplicationContext())

        assertFalse(prefs.permissionAsked())
    }

    @Test
    fun `setPermissionAsked persists true`() {
        val prefs = SharedPreferencesCalendarPrefs(ApplicationProvider.getApplicationContext())

        prefs.setPermissionAsked()

        assertTrue(prefs.permissionAsked())
    }

    @Test
    fun `a new instance over the same context sees a prior setPermissionAsked`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferencesCalendarPrefs(context).setPermissionAsked()

        val secondInstance = SharedPreferencesCalendarPrefs(context)

        assertTrue(secondInstance.permissionAsked())
    }
}
