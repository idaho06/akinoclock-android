package org.akinosoft.akinoclock.app

import android.app.Application

class AkinoClockApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        ThemeApplier.apply(this, container.settingsRepository.currentThemeMode())
    }
}
