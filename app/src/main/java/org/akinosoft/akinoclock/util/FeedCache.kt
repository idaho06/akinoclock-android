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
        val key = keyFor(url)
        val target = bodyFile(key)
        writeAtomically(target, bytes)
        target.setLastModified(nowMillis())
        writeValidators(key, validators)
    }

    fun readValidators(url: String): CacheValidators? {
        val file = validatorsFile(keyFor(url))
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
        val file = migrateLegacyFileIfPresent(keyFor(url))
        if (!file.isFile) return null
        return try {
            CachedFeed(file.readBytes(), file.lastModified())
        } catch (e: IOException) {
            null
        }
    }

    fun clear(url: String) {
        val key = keyFor(url)
        bodyFile(key).delete()
        validatorsFile(key).delete()
    }

    /**
     * Returns the current cache file for [key], migrating a pre-`.cache`-extension file left
     * over from before FeedCache was shared between RSS and weather, if one is found.
     */
    private fun migrateLegacyFileIfPresent(key: String): File {
        val file = bodyFile(key)
        if (file.isFile) return file
        val legacyFile = File(cacheDir, "$key.xml")
        if (!legacyFile.isFile) return file
        return if (legacyFile.renameTo(file)) file else legacyFile
    }

    /** One header value per line (empty when absent); header values never contain newlines. */
    private fun writeValidators(key: String, validators: CacheValidators?) {
        val file = validatorsFile(key)
        if (validators == null) {
            file.delete()
            return
        }
        writeAtomically(file, "${validators.etag.orEmpty()}\n${validators.lastModified.orEmpty()}\n".toByteArray())
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(cacheDir, "${target.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("failed to rename $tmp to $target")
        }
    }

    private fun bodyFile(key: String) = File(cacheDir, "$key.cache")

    private fun validatorsFile(key: String) = File(cacheDir, "$key.meta")

    private fun keyFor(url: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
