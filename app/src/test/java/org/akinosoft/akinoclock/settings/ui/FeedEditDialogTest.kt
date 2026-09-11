package org.akinosoft.akinoclock.settings.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.settings.logic.FeedUrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedEditDialogTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `adding a feed starts with an empty input`() {
        val dialog = FeedEditDialog.show(context(), existingUrl = null) { FeedUrlValidator.Result.Valid(it) }

        assertEquals("", dialog.findViewById<EditText>(R.id.feedUrlInput)!!.text.toString())
    }

    @Test
    fun `editing a feed pre-fills the input with the existing URL`() {
        val dialog = FeedEditDialog.show(context(), existingUrl = "https://example.com/a.xml") {
            FeedUrlValidator.Result.Valid(it)
        }

        assertEquals(
            "https://example.com/a.xml",
            dialog.findViewById<EditText>(R.id.feedUrlInput)!!.text.toString(),
        )
    }

    @Test
    fun `saving an invalid URL shows an inline error and does not dismiss`() {
        val dialog = FeedEditDialog.show(context(), existingUrl = null) {
            FeedUrlValidator.Result.Invalid("bad url")
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

        assertEquals("bad url", dialog.findViewById<EditText>(R.id.feedUrlInput)!!.error)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `saving a valid URL calls onSave and dismisses`() {
        var savedUrl: String? = null
        val dialog = FeedEditDialog.show(context(), existingUrl = null) { url ->
            savedUrl = url
            FeedUrlValidator.Result.Valid(url)
        }
        dialog.findViewById<EditText>(R.id.feedUrlInput)!!.setText("https://example.com/a.xml")

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

        assertEquals("https://example.com/a.xml", savedUrl)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `cancel dismisses without calling onSave`() {
        var called = false
        val dialog = FeedEditDialog.show(context(), existingUrl = null) {
            called = true
            FeedUrlValidator.Result.Valid(it)
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()

        assertFalse(called)
        assertFalse(dialog.isShowing)
    }
}
