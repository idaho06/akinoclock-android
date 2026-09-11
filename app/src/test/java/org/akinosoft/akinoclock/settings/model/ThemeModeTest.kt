package org.akinosoft.akinoclock.settings.model

import android.app.UiModeManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `maps to the UiModeManager night mode constants`() {
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, ThemeMode.SYSTEM.toUiModeManagerConstant())
        assertEquals(UiModeManager.MODE_NIGHT_NO, ThemeMode.LIGHT.toUiModeManagerConstant())
        assertEquals(UiModeManager.MODE_NIGHT_YES, ThemeMode.DARK.toUiModeManagerConstant())
    }

    @Test
    fun `fromStorageString round-trips each mode's storage value`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageString("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageString("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageString("dark"))
    }

    @Test
    fun `fromStorageString defaults to SYSTEM for unknown or missing values`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageString("bogus"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageString(null))
    }
}
