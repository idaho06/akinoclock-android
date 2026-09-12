package org.akinosoft.akinoclock.settings.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.weather.model.WeatherLocation

/**
 * A city-search dialog for picking the weather location. Search is inherently async (a network
 * call through the ViewModel's one-shot `searchResult` flow), so this dialog doesn't own the
 * result: [show] only validates the query and forwards it via [onSearch], and the caller drives
 * [showInlineError] / [showResults] once the ViewModel's flow emits.
 */
object LocationSearchDialog {

    fun show(context: Context, onSearch: (String) -> Unit): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_location_search, null)
        val input = view.findViewById<EditText>(R.id.locationQueryInput)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.settings_weather_location_change)
            .setView(view)
            .setPositiveButton(R.string.settings_weather_location_search, null)
            .setNegativeButton(R.string.settings_dialog_cancel, null)
            .create()

        // Overridden after show(), like FeedEditDialog, so an empty query keeps the dialog open
        // with an inline error instead of auto-dismissing.
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val query = input.text.toString()
            if (query.isBlank()) {
                input.error = context.getString(R.string.settings_weather_location_empty_query)
            } else {
                onSearch(query)
            }
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { dialog.dismiss() }

        return dialog
    }

    fun showInlineError(dialog: AlertDialog, message: String) {
        dialog.findViewById<EditText>(R.id.locationQueryInput)?.error = message
    }

    fun showResults(context: Context, dialog: AlertDialog, locations: List<WeatherLocation>, onPick: (WeatherLocation) -> Unit) {
        dialog.dismiss()
        val labels = locations.map { it.name }.toTypedArray()
        AlertDialog.Builder(context)
            .setItems(labels) { _, index -> onPick(locations[index]) }
            .show()
    }
}
