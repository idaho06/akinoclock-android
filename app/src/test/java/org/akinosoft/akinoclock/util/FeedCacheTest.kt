package org.akinosoft.akinoclock.util

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.akinosoft.akinoclock.util.net.CacheValidators
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FeedCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun cache(nowMillis: Long = 1_000L) =
        FeedCache(tempFolder.newFolder("rss-cache")) { nowMillis }

    @Test
    fun `write then read returns identical bytes`() {
        val cache = cache()
        val bytes = "<rss></rss>".toByteArray()

        cache.write("https://example.com/feed.xml", bytes)
        val result = cache.read("https://example.com/feed.xml")

        assertArrayEquals(bytes, result?.bytes)
    }

    @Test
    fun `fetchedAt is the injected clock's time at write`() {
        val cache = cache(nowMillis = 12_345L)

        cache.write("https://example.com/feed.xml", "x".toByteArray())
        val result = cache.read("https://example.com/feed.xml")

        assertEquals(12_345L, result?.fetchedAtMillis)
    }

    @Test
    fun `cache key is stable for the same URL and different for different URLs`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }

        cache.write("https://example.com/a.xml", "a".toByteArray())
        cache.write("https://example.com/a.xml", "a-again".toByteArray())
        assertEquals(1, dir.listFiles()?.size)

        cache.write("https://example.com/b.xml", "b".toByteArray())
        assertEquals(2, dir.listFiles()?.size)
    }

    @Test
    fun `write is atomic and leaves no tmp file behind`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }

        cache.write("https://example.com/a.xml", "a".toByteArray())

        val leftoverTmp = dir.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertTrue(leftoverTmp.isEmpty())
    }

    @Test
    fun `a failed write does not corrupt the previously cached file`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }
        val url = "https://example.com/a.xml"
        cache.write(url, "original".toByteArray())

        dir.setWritable(false)
        val probe = File(dir, "probe-writability")
        val directoryIsActuallyReadOnly = runCatching { probe.createNewFile() }.getOrDefault(false).not()
        probe.delete()
        dir.setWritable(true)
        assumeTrue("test runner does not honor read-only directories (likely running as root)", directoryIsActuallyReadOnly)

        dir.setWritable(false)
        try {
            cache.write(url, "corrupted".toByteArray())
        } catch (_: Exception) {
            // A failed write may throw; what matters is the previous file survives untouched.
        } finally {
            dir.setWritable(true)
        }

        assertArrayEquals("original".toByteArray(), cache.read(url)?.bytes)
    }

    @Test
    fun `touch bumps mtime without changing the cached bytes`() {
        val dir = tempFolder.newFolder("rss-cache")
        var currentMillis = 1_000L
        val cache = FeedCache(dir) { currentMillis }
        val url = "https://example.com/feed.xml"
        cache.write(url, "original".toByteArray())

        currentMillis = 5_000L
        cache.touch(url)

        val result = cache.read(url)
        assertArrayEquals("original".toByteArray(), result?.bytes)
        assertEquals(5_000L, result?.fetchedAtMillis)
    }

    @Test
    fun `touch on an absent entry does nothing`() {
        val cache = cache()

        cache.touch("https://example.com/never-written.xml")

        assertNull(cache.read("https://example.com/never-written.xml"))
    }

    @Test
    fun `clear deletes the cached file`() {
        val cache = cache()
        val url = "https://example.com/feed.xml"
        cache.write(url, "x".toByteArray())

        cache.clear(url)

        assertNull(cache.read(url))
    }

    @Test
    fun `read returns null for an absent file`() {
        val cache = cache()

        assertNull(cache.read("https://example.com/never-written.xml"))
    }

    @Test
    fun `read migrates a legacy xml-suffixed cache file to the cache extension`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }
        val url = "https://example.com/feed.xml"
        val legacyFile = File(dir, sha1Hex(url) + ".xml")
        legacyFile.writeBytes("legacy".toByteArray())

        val result = cache.read(url)

        assertArrayEquals("legacy".toByteArray(), result?.bytes)
        assertTrue(!legacyFile.isFile)
        assertTrue(File(dir, sha1Hex(url) + ".cache").isFile)
    }

    @Test
    fun `read still returns legacy bytes when the migrating rename fails`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }
        val url = "https://example.com/feed.xml"
        val legacyFile = File(dir, sha1Hex(url) + ".xml")
        legacyFile.writeBytes("legacy".toByteArray())

        dir.setWritable(false)
        val probe = File(dir, "probe-writability")
        val directoryIsActuallyReadOnly = runCatching { probe.createNewFile() }.getOrDefault(false).not()
        probe.delete()
        dir.setWritable(true)
        assumeTrue("test runner does not honor read-only directories (likely running as root)", directoryIsActuallyReadOnly)

        dir.setWritable(false)
        val result = try {
            cache.read(url)
        } finally {
            dir.setWritable(true)
        }

        assertArrayEquals("legacy".toByteArray(), result?.bytes)
    }

    private fun sha1Hex(text: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `read returns null for a corrupt cache entry`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }
        val url = "https://example.com/feed.xml"
        cache.write(url, "x".toByteArray())

        // Replace the cached file with a directory to simulate an unreadable/corrupt entry.
        val cachedFile = dir.listFiles()!!.single()
        cachedFile.delete()
        cachedFile.mkdir()

        assertNull(cache.read(url))
    }

    @Test
    fun `validators written with the bytes are read back verbatim`() {
        val cache = cache()
        val url = "https://example.com/feed.xml"
        val validators = CacheValidators(etag = "W/\"abc-123\"", lastModified = "Sat, 26 Sep 2026 10:00:00 GMT")

        cache.write(url, "x".toByteArray(), validators)

        assertEquals(validators, cache.readValidators(url))
    }

    @Test
    fun `a validator the server did not send is read back as null`() {
        val cache = cache()
        val url = "https://example.com/feed.xml"

        cache.write(url, "x".toByteArray(), CacheValidators(etag = "\"abc\"", lastModified = null))

        assertEquals(CacheValidators(etag = "\"abc\"", lastModified = null), cache.readValidators(url))
    }

    @Test
    fun `readValidators is null when nothing was stored`() {
        val cache = cache()
        val url = "https://example.com/feed.xml"

        assertNull(cache.readValidators(url))
        cache.write(url, "x".toByteArray())
        assertNull(cache.readValidators(url))
    }

    @Test
    fun `rewriting without validators drops the previously stored ones`() {
        val cache = cache()
        val url = "https://example.com/feed.xml"
        cache.write(url, "x".toByteArray(), CacheValidators(etag = "\"abc\"", lastModified = null))

        cache.write(url, "y".toByteArray())

        assertNull(cache.readValidators(url))
    }

    @Test
    fun `clear also deletes the stored validators`() {
        val dir = tempFolder.newFolder("rss-cache")
        val cache = FeedCache(dir) { 1_000L }
        val url = "https://example.com/feed.xml"
        cache.write(url, "x".toByteArray(), CacheValidators(etag = "\"abc\"", lastModified = null))

        cache.clear(url)

        assertNull(cache.readValidators(url))
        assertEquals(0, dir.listFiles()?.size)
    }
}
