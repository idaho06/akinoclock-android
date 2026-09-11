package org.akinosoft.akinoclock.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** Runs [block] under `runTest` with `Dispatchers.Main` set to a [StandardTestDispatcher] tied to
 * the test's virtual scheduler. Needed by any ViewModel test using `viewModelScope`, whose `Main`
 * dispatcher resolution requires a working `Looper.getMainLooper()` under Robolectric. */
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
