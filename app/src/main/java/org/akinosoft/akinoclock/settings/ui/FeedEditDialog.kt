package org.akinosoft.akinoclock.settings.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.settings.logic.FeedUrlValidator

/** Shows an add/edit dialog for a single feed URL. [onSave] validates and persists; on
 * [FeedUrlValidator.Result.Invalid] the dialog stays open with the reason shown inline. */
object FeedEditDialog {

    fun show(context: Context, existingUrl: String?, onSave: (String) -> FeedUrlValidator.Result): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_feed_edit, null)
        val input = view.findViewById<EditText>(R.id.feedUrlInput)
        input.setText(existingUrl.orEmpty())

        val titleRes = if (existingUrl == null) R.string.settings_add_feed_title else R.string.settings_edit_feed_title
        val dialog = AlertDialog.Builder(context)
            .setTitle(titleRes)
            .setView(view)
            .setPositiveButton(R.string.settings_dialog_save, null)
            .setNegativeButton(R.string.settings_dialog_cancel, null)
            .create()

        // Overriding the positive button's click listener directly (rather than via
        // setOnShowListener, whose delivery is posted through a Handler) keeps a failed
        // validation from auto-dismissing the dialog, the default AlertDialog behavior.
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            when (val result = onSave(input.text.toString())) {
                is FeedUrlValidator.Result.Invalid -> input.error = result.reason
                is FeedUrlValidator.Result.Valid -> dialog.dismiss()
            }
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { dialog.dismiss() }

        return dialog
    }
}
