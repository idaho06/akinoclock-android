package org.akinosoft.akinoclock.util

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshBackoffTest {

    @Test
    fun `normal interval when not backing off`() {
        val backoff = RefreshBackoff(normalInterval = Duration.ofMinutes(30))
        assertEquals(Duration.ofMinutes(30), backoff.nextDelay())
    }

    @Test
    fun `failures step through the backoff sequence and cap at the last entry`() {
        val backoff = RefreshBackoff(normalInterval = Duration.ofMinutes(30))

        backoff.onFailure()
        assertEquals(Duration.ofMinutes(1), backoff.nextDelay())
        backoff.onFailure()
        assertEquals(Duration.ofMinutes(2), backoff.nextDelay())
        backoff.onFailure()
        assertEquals(Duration.ofMinutes(4), backoff.nextDelay())
        backoff.onFailure()
        assertEquals(Duration.ofMinutes(8), backoff.nextDelay())
        backoff.onFailure()
        assertEquals(Duration.ofMinutes(15), backoff.nextDelay())
        backoff.onFailure()
        assertEquals(Duration.ofMinutes(15), backoff.nextDelay())
    }

    @Test
    fun `success resets to the normal interval`() {
        val backoff = RefreshBackoff(normalInterval = Duration.ofMinutes(30))

        backoff.onFailure()
        backoff.onFailure()
        backoff.onSuccess()

        assertEquals(Duration.ofMinutes(30), backoff.nextDelay())
    }

    @Test
    fun `reset behaves like a fresh backoff`() {
        val backoff = RefreshBackoff(normalInterval = Duration.ofMinutes(30))

        backoff.onFailure()
        backoff.reset()

        assertEquals(Duration.ofMinutes(30), backoff.nextDelay())
    }

    @Test
    fun `isBackingOff reflects whether the last outcome was a failure`() {
        val backoff = RefreshBackoff(normalInterval = Duration.ofMinutes(30))
        assertEquals(false, backoff.isBackingOff())

        backoff.onFailure()
        assertEquals(true, backoff.isBackingOff())

        backoff.onSuccess()
        assertEquals(false, backoff.isBackingOff())
    }
}
