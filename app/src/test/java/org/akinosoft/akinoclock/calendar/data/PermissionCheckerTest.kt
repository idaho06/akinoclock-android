package org.akinosoft.akinoclock.calendar.data

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PermissionCheckerTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val checker = ContextPermissionChecker(application)

    @Test
    fun `hasReadCalendar is false when the permission has not been granted`() {
        shadowOf(application).denyPermissions(Manifest.permission.READ_CALENDAR)

        assertFalse(checker.hasReadCalendar())
    }

    @Test
    fun `hasReadCalendar is true once the permission is granted`() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_CALENDAR)

        assertTrue(checker.hasReadCalendar())
    }
}
