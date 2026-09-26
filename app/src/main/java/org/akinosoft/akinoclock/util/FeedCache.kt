package org.akinosoft.akinoclock.util

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import org.akinosoft.akinoclock.util.net.CacheValidators

data class CachedFeed(val bytes: ByteArray, val fetchedAtMillis: Long)

/**
 * Raw response bytes cached on disk under [cacheDir], keyed by a hash of the source URL so
 * cache files survive restarts without needing an index. Writes are tmp-file-then-rename so a
 * failed write never corrupts what was cached before it. No expiry: stale data with an
 * indicator beats an empty carousel. Shared by RSS and weather, so cache files use a generic
 * extension rather than one tied to either format. HTTP [CacheValidators] live in a small sidecar
 * file next to the body, so reading them never loads the cached bytes.
 */
class FeedCache(
    private val cacheDir: File,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {

    fun write(url: String, bytes: ByteArray, validators: CacheValidators? = null) {
        cacheDir.mkdirs()
        val target = fileFor(url)
        val tmp = File(cacheDir, "${target.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("failed to rename $tmp to $target")
        }
        target.setLastModified(nowMillis())
        writeValidators(url, validators)
    }

    fun readValidators(url: String): CacheValidators? {
        val file = validatorsFileFor(url)
        if (!file.isFile) return null
        return try {
            val lines = file.readLines()
            CacheValidators(
                etag = lines.getOrNull(0)?.takeIf { it.isNotEmpty() },
                lastModified = lines.getOrNull(1)?.takeIf { it.isNotEmpty() },
            )
        } catch (e: IOException) {
            null
        }
    }

    fun read(url: String): CachedFeed? {
        val file = migrateLegacyFileIfPresent(url)
        if (!file.isFile) return null
        return try {
            CachedFeed(file.readBytes(), file.lastModified())
        } catch (e: IOException) {
            null
        }
    }

    fun clear(url: String) {
        fileFor(url).delete()
        validatorsFileFor(url).delete()
    }

    /** Bumps a cached entry's mtime without rewriting its bytes, e.g. after a 304 response. */
    fun touch(url: String) {
        val file = fileFor(url)
        if (file.isFile) file.setLastModified(nowMillis())
    }

    /**
     * Returns the current cache file for [url], migrating a pre-`.cache`-extension file left
     * over from before FeedCache was shared between RSS and weather, if one is found.
     */
    private fun migrateLegacyFileIfPresent(url: String): File {
        val file = fileFor(url)
        if (file.isFile) return file
        val legacyFile = File(cacheDir, "${sha1Hex(url)}.xml")
        if (!legacyFile.isFile) return file
        return if (legacyFile.renameTo(file)) file else legacyFile
    }

    /** One header value per line (empty when absent); header values never contain newlines. */
    private fun writeValidators(url: String, validators: CacheValidators?) {
        val file = validatorsFileFor(url)
        if (validators == null || (validators.etag == null && validators.lastModified == null)) {
            file.delete()
            return
        }
        file.writeText("${validators.etag.orEmpty()}\n${validators.lastModified.orEmpty()}\n")
    }

    private fun fileFor(url: String) = File(cacheDir, "${sha1Hex(url)}.cache")

    private fun validatorsFileFor(url: String) = File(cacheDir, "${sha1Hex(url)}.meta")

    private fun sha1Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
