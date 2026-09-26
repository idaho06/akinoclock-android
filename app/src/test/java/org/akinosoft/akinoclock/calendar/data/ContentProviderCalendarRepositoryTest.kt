package org.akinosoft.akinoclock.calendar.data

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.akinosoft.akinoclock.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ContentProviderCalendarRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: ContentProviderCalendarRepository
    private lateinit var previousDefaultZone: TimeZone

    @Before
    fun setUp() {
        previousDefaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
        context = ApplicationProvider.getApplicationContext()
        Robolectric.setupContentProvider(FakeCalendarProvider::class.java, "com.android.calendar")
        FakeCalendarProvider.reset()
        repository = ContentProviderCalendarRepository(context, ioDispatcher = UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousDefaultZone)
    }

    private fun emptyCursor() = MatrixCursor(
        arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        ),
    )

    @Test
    fun `queries the Instances URI with begin and end appended as path segments`() = runTest {
        FakeCalendarProvider.cursorToReturn = emptyCursor()

        repository.instancesBetween(1000L, 2000L)

        assertEquals(
            Uri.parse("content://com.android.calendar/instances/when/1000/2000"),
            FakeCalendarProvider.lastQueriedUri,
        )
    }

    @Test
    fun `maps a timed row to an EventInstance in the system default zone`() = runTest {
        val madrid = ZoneId.of("Europe/Madrid")
        val begin = ZonedDateTime.of(2026, 9, 25, 18, 0, 0, 0, madrid).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 9, 25, 19, 0, 0, 0, madrid).toInstant().toEpochMilli()
        FakeCalendarProvider.cursorToReturn = emptyCursor().apply {
            addRow(arrayOf<Any?>(1L, "Standup", begin, end, 0))
        }

        val instance = repository.instancesBetween(0L, Long.MAX_VALUE).single()

        assertEquals(1L, instance.id)
        assertEquals("Standup", instance.title)
        assertFalse(instance.allDay)
        assertEquals(madrid, instance.start.zone)
        assertEquals(18, instance.start.hour)
    }

    @Test
    fun `maps an all-day row to an EventInstance anchored in UTC, not the system zone`() = runTest {
        val begin = ZonedDateTime.of(2026, 9, 26, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        FakeCalendarProvider.cursorToReturn = emptyCursor().apply {
            addRow(arrayOf<Any?>(2L, "Cumpleaños Ester", begin, end, 1))
        }

        val instance = repository.instancesBetween(0L, Long.MAX_VALUE).single()

        assertTrue(instance.allDay)
        assertEquals(ZoneOffset.UTC, instance.start.zone)
        assertEquals(2026, instance.start.year)
        assertEquals(9, instance.start.monthValue)
        assertEquals(26, instance.start.dayOfMonth)
    }

    @Test
    fun `a null title maps to the no-title string resource`() = runTest {
        FakeCalendarProvider.cursorToReturn = emptyCursor().apply {
            addRow(arrayOf(3L, null, 0L, 0L, 0))
        }

        val instance = repository.instancesBetween(0L, Long.MAX_VALUE).single()

        assertEquals(context.getString(R.string.calendar_no_title), instance.title)
    }

    @Test
    fun `a SecurityException from the resolver surfaces as an empty list`() = runTest {
        FakeCalendarProvider.throwSecurityException = true

        val result = repository.instancesBetween(0L, Long.MAX_VALUE)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `changes emits when the registered ContentObserver fires`() = runTest {
        val emitted = mutableListOf<Unit>()
        val job = launch { repository.changes().collect { emitted.add(it) } }
        advanceUntilIdle()

        shadowOf(context.contentResolver)
            .getContentObservers(CalendarContract.CONTENT_URI)
            .forEach { it.dispatchChange(false, CalendarContract.CONTENT_URI) }
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        job.cancel()
    }

    @Test
    fun `changes does not crash when registerContentObserver throws SecurityException`() = runTest {
        // On the device, registerContentObserver on the CalendarContract authority
        // throws SecurityException immediately when READ_CALENDAR isn't granted (unlike a
        // query, which the provider just fails). The Flow must not propagate that exception
        // to its collector.
        val contentResolver = mockk<ContentResolver> {
            every { registerContentObserver(any(), any(), any()) } throws SecurityException("revoked")
            every { unregisterContentObserver(any()) } returns Unit
        }
        val revokedContext = mockk<Context> { every { this@mockk.contentResolver } returns contentResolver }
        val revokedRepository = ContentProviderCalendarRepository(revokedContext, ioDispatcher = UnconfinedTestDispatcher())

        val emitted = mutableListOf<Unit>()
        val job = launch { revokedRepository.changes().collect { emitted.add(it) } }
        advanceUntilIdle()

        assertTrue(emitted.isEmpty())
        job.cancel()
    }
}
