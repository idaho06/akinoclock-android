package org.akinosoft.akinoclock.settings.logic

import java.net.URI
import java.net.URISyntaxException
import org.akinosoft.akinoclock.rss.model.FeedConfig

/** Pure normalize + validate + duplicate-check logic for user-entered feed URLs. Uses
 * [java.net.URI] rather than `android.net.Uri` so this stays plain-JUnit testable. */
object FeedUrlValidator {

    sealed interface Result {
        data class Valid(val url: String) : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(input: String, existingFeeds: List<FeedConfig>, excluding: String? = null): Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.Invalid("URL is required")

        val normalized = if (SCHEME_PREFIX.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val uri = try {
            URI(normalized)
        } catch (e: URISyntaxException) {
            return Result.Invalid("Not a valid URL")
        }

        if (uri.scheme?.lowercase() !in ALLOWED_SCHEMES) return Result.Invalid("Only http/https URLs are supported")
        if (uri.host.isNullOrBlank()) return Result.Invalid("URL must include a host")

        val key = duplicateKey(uri)
        val isDuplicate = existingFeeds
            .filter { it.url != excluding }
            .any { duplicateKey(URI(it.url)) == key }
        if (isDuplicate) return Result.Invalid("This feed is already in the list")

        return Result.Valid(normalized)
    }

    private fun duplicateKey(uri: URI): String {
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty().trimEnd('/')
        return "$host$path"
    }

    private val SCHEME_PREFIX = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
    private val ALLOWED_SCHEMES = setOf("http", "https")
}
