package org.akinosoft.akinoclock.util

import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class CachedFeed(val bytes: ByteArray, val fetchedAtMillis: Long)

/**
 * Raw response bytes cached on disk under [cacheDir], keyed by a hash of the source URL so
 * cache files survive restarts without needing an index. Writes are tmp-file-then-rename so a
 * failed write never corrupts what was cached before it. No expiry: stale data with an
 * indicator beats an empty carousel. Shared by RSS and weather, so cache files use a generic
 * extension rather than one tied to either format.
 */
class FeedCache(
    private val cacheDir: File,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {

    fun write(url: String, bytes: ByteArray) {
        cacheDir.mkdirs()
        val target = fileFor(url)
        val tmp = File(cacheDir, "${target.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("failed to rename $tmp to $target")
        }
        target.setLastModified(nowMillis())
    }

    fun read(url: String): CachedFeed? {
        val file = fileFor(url)
        if (!file.isFile) return null
        return try {
            CachedFeed(file.readBytes(), file.lastModified())
        } catch (e: IOException) {
            null
        }
    }

    fun clear(url: String) {
        fileFor(url).delete()
    }

    /** Bumps a cached entry's mtime without rewriting its bytes, e.g. after a 304 response. */
    fun touch(url: String) {
        val file = fileFor(url)
        if (file.isFile) file.setLastModified(nowMillis())
    }

    private fun fileFor(url: String) = File(cacheDir, "${sha1Hex(url)}.cache")

    private fun sha1Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
