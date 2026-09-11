package org.akinosoft.akinoclock.settings.logic

import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedUrlValidatorTest {

    private fun assertValid(input: String, expectedUrl: String, existing: List<FeedConfig> = emptyList()) {
        val result = FeedUrlValidator.validate(input, existing)
        assertTrue("expected Valid for '$input' but was $result", result is FeedUrlValidator.Result.Valid)
        assertEquals(expectedUrl, (result as FeedUrlValidator.Result.Valid).url)
    }

    private fun assertInvalid(input: String, existing: List<FeedConfig> = emptyList()) {
        val result = FeedUrlValidator.validate(input, existing)
        assertTrue("expected Invalid for '$input' but was $result", result is FeedUrlValidator.Result.Invalid)
    }

    @Test
    fun `a bare host and path is normalized to https`() {
        assertValid("example.com/feed", "https://example.com/feed")
    }

    @Test
    fun `an explicit http scheme is kept`() {
        assertValid("http://a/b", "http://a/b")
    }

    @Test
    fun `a non-http scheme is rejected`() {
        assertInvalid("ftp://x")
    }

    @Test
    fun `blank input is rejected`() {
        assertInvalid("   ")
    }

    @Test
    fun `a scheme with no host is rejected`() {
        assertInvalid("https://")
    }

    @Test
    fun `a case-insensitive duplicate of an existing feed is rejected`() {
        val existing = listOf(FeedConfig(url = "https://A.com/x"))

        assertInvalid("https://a.com/x", existing)
    }

    @Test
    fun `a new URL not already present is accepted`() {
        val existing = listOf(FeedConfig(url = "https://a.com/x"))

        assertValid("https://a.com/y", "https://a.com/y", existing)
    }

    @Test
    fun `excluding a URL from the duplicate check allows editing it in place`() {
        val existing = listOf(FeedConfig(url = "https://a.com/x"))

        val result = FeedUrlValidator.validate("https://a.com/x", existing, excluding = "https://a.com/x")

        assertTrue(result is FeedUrlValidator.Result.Valid)
    }
}
