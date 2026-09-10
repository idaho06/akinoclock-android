package org.akinosoft.akinoclock

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CoroutinesSanityTest {

    @Test
    fun `runTest resolves with a StandardTestDispatcher`() = runTest(StandardTestDispatcher()) {
        var value = 0
        value = 1

        assertEquals(1, value)
    }
}
