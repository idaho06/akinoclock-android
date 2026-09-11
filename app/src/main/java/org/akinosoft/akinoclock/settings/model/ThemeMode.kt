package org.akinosoft.akinoclock.settings.model

import android.app.UiModeManager

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    fun toUiModeManagerConstant(): Int = when (this) {
        SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        LIGHT -> UiModeManager.MODE_NIGHT_NO
        DARK -> UiModeManager.MODE_NIGHT_YES
    }

    companion object {
        fun fromStorageString(value: String?): ThemeMode = entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
