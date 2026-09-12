package org.akinosoft.akinoclock.settings.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
class LocationSearchDialogTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an empty query shows an inline error and does not call onSearch`() {
        var called = false
        val dialog = LocationSearchDialog.show(context()) { called = true }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

        assertFalse(called)
        assertEquals(
            context().getString(R.string.settings_weather_location_empty_query),
            dialog.findViewById<EditText>(R.id.locationQueryInput)!!.error,
        )
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `a non-empty query calls onSearch and keeps the dialog open pending the async result`() {
        var searched: String? = null
        val dialog = LocationSearchDialog.show(context()) { searched = it }
        dialog.findViewById<EditText>(R.id.locationQueryInput)!!.setText("Madrid")

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

        assertEquals("Madrid", searched)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `cancel dismisses without calling onSearch`() {
        var called = false
        val dialog = LocationSearchDialog.show(context()) { called = true }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()

        assertFalse(called)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `showInlineError sets the input's error`() {
        val dialog = LocationSearchDialog.show(context()) {}

        LocationSearchDialog.showInlineError(dialog, "No places found")

        assertEquals("No places found", dialog.findViewById<EditText>(R.id.locationQueryInput)!!.error)
    }

    @Test
    fun `showResults dismisses the search dialog and lets picking a result invoke onPick`() {
        val dialog = LocationSearchDialog.show(context()) {}
        val madrid = WeatherLocation("Madrid, Spain", 40.4165, -3.70256)
        val paris = WeatherLocation("Paris, France", 48.8566, 2.3522)
        var picked: WeatherLocation? = null

        LocationSearchDialog.showResults(context(), dialog, listOf(madrid, paris)) { picked = it }

        assertFalse(dialog.isShowing)
        val resultsDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(resultsDialog.isShowing)
        shadowOf(resultsDialog).clickOnItem(1)
        assertEquals(paris, picked)
    }
}
