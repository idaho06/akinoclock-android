package org.akinosoft.akinoclock.calendar.ui

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.calendar.model.CalendarUiState
import org.akinosoft.akinoclock.calendar.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CalendarViewModelTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    private val allDayInstance = EventInstance(
        id = 1,
        title = "Cumpleaños Ester",
        start = ZonedDateTime.of(2026, 9, 26, 0, 0, 0, 0, ZoneOffset.UTC),
        end = ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, ZoneOffset.UTC),
        allDay = true,
    )

    private fun clockAt(date: LocalDate, hour: Int = 12): Clock =
        Clock.fixed(ZonedDateTime.of(date, java.time.LocalTime.of(hour, 0), madrid).toInstant(), madrid)

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `permission denied yields NotGranted and never queries the repository`() = runViewModelTest {
        val repository = mockk<CalendarRepository> {
            every { changes() } returns emptyFlow()
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns false }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CalendarUiState.NotGranted
        assertEquals(42, state.grid.cells.size)
        assertTrue(state.grid.cells.none { it.hasEvents })
        coVerify(exactly = 0) { repository.instancesBetween(any(), any()) }
    }

    @Test
    fun `permission granted emits Loading then Granted with grid and today list`() = runViewModelTest {
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } coAnswers {
                delay(10)
                listOf(allDayInstance)
            }
            every { changes() } returns emptyFlow()
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val states = mutableListOf<CalendarUiState>()
        val job = launch { viewModel.uiState.toList(states) }
        viewModel.start()
        advanceUntilIdle()
        job.cancel()

        assertEquals(CalendarUiState.Loading, states.first())
        val granted = states.last() as CalendarUiState.Granted
        assertTrue(granted.grid.cells.first { it.date == LocalDate.of(2026, 9, 26) }.hasEvents)
        assertEquals(listOf(allDayInstance), granted.todayList)
    }

    @Test
    fun `refresh re-queries the repository`() = runViewModelTest {
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } returns emptyList()
            every { changes() } returns emptyFlow()
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.instancesBetween(any(), any()) }
    }

    @Test
    fun `a changes emission re-queries the repository`() = runViewModelTest {
        val changes = MutableSharedFlow<Unit>()
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } returns emptyList()
            every { changes() } returns changes
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()

        changes.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.instancesBetween(any(), any()) }
    }

    @Test
    fun `stop cancels the changes subscription so a later emission does not re-query`() = runViewModelTest {
        val changes = MutableSharedFlow<Unit>()
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } returns emptyList()
            every { changes() } returns changes
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()

        viewModel.stop()
        changes.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.instancesBetween(any(), any()) }
    }

    @Test
    fun `start after stop resubscribes to changes`() = runViewModelTest {
        val changes = MutableSharedFlow<Unit>()
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } returns emptyList()
            every { changes() } returns changes
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()
        viewModel.stop()

        viewModel.start()
        advanceUntilIdle()
        changes.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 3) { repository.instancesBetween(any(), any()) }
    }

    @Test
    fun `a clock advanced past midnight moves isToday to the new day on the next refresh`() = runViewModelTest {
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } returns emptyList()
            every { changes() } returns emptyFlow()
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }
        var now = Instant.parse("2026-09-25T21:59:00Z")
        val clock = object : Clock() {
            override fun instant() = now
            override fun getZone() = madrid
            override fun withZone(zone: ZoneId) = this
        }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clock,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()

        val firstGrid = (viewModel.uiState.value as CalendarUiState.Granted).grid
        assertTrue(firstGrid.cells.first { it.date == LocalDate.of(2026, 9, 25) }.isToday)

        now = Instant.parse("2026-09-25T22:01:00Z") // 2026-09-26T00:01 in Madrid (+02:00)
        viewModel.refresh()
        advanceUntilIdle()

        val secondGrid = (viewModel.uiState.value as CalendarUiState.Granted).grid
        assertFalse(secondGrid.cells.first { it.date == LocalDate.of(2026, 9, 25) }.isToday)
        assertTrue(secondGrid.cells.first { it.date == LocalDate.of(2026, 9, 26) }.isToday)
    }

    @Test
    fun `errors from the repository do not crash and keep the previous state`() = runViewModelTest {
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } returns listOf(allDayInstance) andThenThrows
                RuntimeException("boom")
            every { changes() } returns emptyFlow()
        }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns true }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()
        val stateAfterFirstLoad = viewModel.uiState.value

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(stateAfterFirstLoad, viewModel.uiState.value)
        assertTrue(viewModel.uiState.value is CalendarUiState.Granted)
    }

    @Test
    fun `a permission revoked mid-query surfaces as NotGranted, not an empty Granted list`() = runViewModelTest {
        val permissionChecker = mockk<PermissionChecker>()
        var granted = true
        every { permissionChecker.hasReadCalendar() } answers { granted }
        val repository = mockk<CalendarRepository> {
            coEvery { instancesBetween(any(), any()) } coAnswers {
                granted = false // simulates ContentProviderCalendarRepository catching a SecurityException
                emptyList()
            }
            every { changes() } returns emptyFlow()
        }

        val viewModel = CalendarViewModel(
            repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        viewModel.start()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CalendarUiState.NotGranted)
    }

    @Test
    fun `the Factory creates a CalendarViewModel`() {
        val repository = mockk<CalendarRepository> { every { changes() } returns emptyFlow() }
        val permissionChecker = mockk<PermissionChecker> { every { hasReadCalendar() } returns false }

        val factory = CalendarViewModel.Factory(repository, permissionChecker, clockAt(LocalDate.of(2026, 9, 25)))
        val viewModel = factory.create(CalendarViewModel::class.java)

        assertEquals(CalendarUiState.Loading, viewModel.uiState.value)
    }
}
