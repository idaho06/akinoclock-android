package org.akinosoft.akinoclock

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MockkSanityTest {

    @Test
    fun `inline mocking works under JDK 21`() {
        val runnable = mockk<Runnable>()
        every { runnable.run() } just Runs

        runnable.run()

        verify { runnable.run() }
    }
}
