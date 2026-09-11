package org.akinosoft.akinoclock.app

import android.app.Application

class AkinoClockApp : Application() {

    private var _container: AppContainer? = null
    var container: AppContainer
        get() = _container ?: AppContainer(this).also { _container = it }
        set(value) { _container = value }

    override fun onCreate() {
        super.onCreate()
        ThemeApplier.apply(this, container.settingsRepository.currentThemeMode())
    }
}
