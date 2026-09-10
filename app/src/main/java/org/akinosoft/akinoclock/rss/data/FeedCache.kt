package org.akinosoft.akinoclock.rss.data

import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class CachedFeed(val bytes: ByteArray, val fetchedAtMillis: Long)

/**
 * Raw feed response bytes cached on disk under [cacheDir], keyed by a hash of the feed URL so
 * cache files survive restarts without needing an index. Writes are tmp-file-then-rename so a
 * failed write never corrupts what was cached before it. No expiry: stale data with an
 * indicator beats an empty carousel.
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

    private fun fileFor(url: String) = File(cacheDir, "${sha1Hex(url)}.xml")

    private fun sha1Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
