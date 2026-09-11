package org.akinosoft.akinoclock.app

import android.app.UiModeManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ThemeApplierTest {

    private fun uiModeManager(): UiModeManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    }

    @Test
    fun `apply sets the UiModeManager application night mode to dark`() {
        ThemeApplier.apply(ApplicationProvider.getApplicationContext(), ThemeMode.DARK)

        assertEquals(UiModeManager.MODE_NIGHT_YES, shadowOf(uiModeManager()).applicationNightMode)
    }

    @Test
    fun `apply sets the UiModeManager application night mode to light`() {
        ThemeApplier.apply(ApplicationProvider.getApplicationContext(), ThemeMode.LIGHT)

        assertEquals(UiModeManager.MODE_NIGHT_NO, shadowOf(uiModeManager()).applicationNightMode)
    }

    @Test
    fun `apply sets the UiModeManager application night mode to system-auto`() {
        ThemeApplier.apply(ApplicationProvider.getApplicationContext(), ThemeMode.SYSTEM)

        assertEquals(UiModeManager.MODE_NIGHT_AUTO, shadowOf(uiModeManager()).applicationNightMode)
    }
}
