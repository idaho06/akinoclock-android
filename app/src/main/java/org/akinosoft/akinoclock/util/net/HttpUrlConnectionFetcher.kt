package org.akinosoft.akinoclock.util.net

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches a resource over HTTP(S) with `HttpURLConnection` — no extra dependency, per the standing
 * "minimal dependencies" constraint. Response bodies are capped at [MAX_BODY_BYTES] while
 * streaming, not after buffering the whole thing into memory.
 */
class HttpUrlConnectionFetcher(
    private val userAgent: String,
    private val accept: String = RSS_ACCEPT_HEADER,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HttpFetcher {

    override suspend fun fetch(url: String, ifModifiedSinceMillis: Long?): FetchResult = withContext(ioDispatcher) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept-Encoding", "gzip")
            if (ifModifiedSinceMillis != null) connection.ifModifiedSince = ifModifiedSinceMillis

            try {
                when (val code = connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> FetchResult.NotModified
                    in 200..299 -> readBody(connection)
                    else -> FetchResult.Failure(FetchResult.Failure.Reason.Http(code))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            FetchResult.Failure(FetchResult.Failure.Reason.Io(e.message))
        }
    }

    private fun readBody(connection: HttpURLConnection): FetchResult {
        val rawStream = connection.inputStream
        val stream = if (connection.contentEncoding?.equals("gzip", ignoreCase = true) == true) {
            GZIPInputStream(rawStream)
        } else {
            rawStream
        }

        // Presize using Content-Length when the server sends one, so a multi-hundred-KB body
        // doesn't grow-and-copy through ByteArrayOutputStream's default 32-byte start size.
        val expectedSize = connection.contentLengthLong.takeIf { it in 1..MAX_BODY_BYTES }?.toInt() ?: 8 * 1024
        val buffer = ByteArrayOutputStream(expectedSize)
        val chunk = ByteArray(8 * 1024)
        var total = 0
        stream.use {
            while (true) {
                val read = it.read(chunk)
                if (read == -1) break
                total += read
                if (total > MAX_BODY_BYTES) return FetchResult.Failure(FetchResult.Failure.Reason.TooLarge)
                buffer.write(chunk, 0, read)
            }
        }
        val lastModified = connection.getHeaderFieldDate("Last-Modified", -1L).takeIf { it > 0 }
        return FetchResult.Success(buffer.toByteArray(), lastModified)
    }

    companion object {
        const val RSS_ACCEPT_HEADER = "application/rss+xml, application/atom+xml, application/xml, text/xml, */*"
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 15_000
        private const val MAX_BODY_BYTES = 2 * 1024 * 1024
    }
}
