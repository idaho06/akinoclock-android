package org.akinosoft.akinoclock.app

import android.app.UiModeManager
import android.content.Context
import org.akinosoft.akinoclock.settings.model.ThemeMode

object ThemeApplier {
    fun apply(context: Context, mode: ThemeMode) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager ?: return
        uiModeManager.setApplicationNightMode(mode.toUiModeManagerConstant())
    }
}
