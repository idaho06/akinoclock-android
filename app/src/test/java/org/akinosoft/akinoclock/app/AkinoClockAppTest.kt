package org.akinosoft.akinoclock.app

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AkinoClockAppTest {

    @Test
    fun `container is available on the application`() {
        val app = ApplicationProvider.getApplicationContext<AkinoClockApp>()

        assertNotNull(app.container)
        assertEquals("org.akinosoft.akinoclock", app.packageName)
    }

    @Test
    fun `runs on the pinned SDK 33`() {
        assertEquals(33, Build.VERSION.SDK_INT)
    }

    @Test
    fun `container is settable for tests`() {
        val app = ApplicationProvider.getApplicationContext<AkinoClockApp>()
        val replacement = AppContainer(app)

        app.container = replacement

        assertEquals(replacement, app.container)
    }
}
