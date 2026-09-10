package org.akinosoft.akinoclock.calendar.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Stands in for the real `com.android.calendar` content provider in Robolectric tests.
 * `ShadowContentResolver.setCursor` is deprecated and does not work with
 * `ContentResolver.acquireContentProviderClient()`, so a real (fake) ContentProvider is
 * installed instead via `Robolectric.setupContentProvider`.
 */
class FakeCalendarProvider : ContentProvider() {

    companion object {
        var cursorToReturn: Cursor? = null
        var throwSecurityException: Boolean = false
        var lastQueriedUri: Uri? = null

        fun reset() {
            cursorToReturn = null
            throwSecurityException = false
            lastQueriedUri = null
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        lastQueriedUri = uri
        if (throwSecurityException) throw SecurityException("READ_CALENDAR revoked")
        return cursorToReturn
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
