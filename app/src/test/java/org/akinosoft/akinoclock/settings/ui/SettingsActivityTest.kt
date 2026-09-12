package org.akinosoft.akinoclock.settings.ui

import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.akinosoft.akinoclock.app.AkinoClockApp
import org.akinosoft.akinoclock.app.AppContainer
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.util.FakeSettingsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {

    private fun installFakeContainer() {
        val app = ApplicationProvider.getApplicationContext<AkinoClockApp>()
        app.container = AppContainer(
            context = app,
            calendarRepository = mockk<CalendarRepository>(relaxed = true),
            permissionChecker = mockk<PermissionChecker>(relaxed = true),
            settingsRepository = FakeSettingsRepository(),
            rssRepository = mockk<RssRepository>(relaxed = true),
        )
    }

    @Test
    fun `destroying the activity dismisses an open location search dialog`() {
        installFakeContainer()
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).create().start().resume()
        val activity = controller.get()

        activity.binding.changeWeatherLocationButton.performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(dialog != null && dialog.isShowing)

        controller.destroy()

        assertFalse(dialog!!.isShowing)
    }
}
