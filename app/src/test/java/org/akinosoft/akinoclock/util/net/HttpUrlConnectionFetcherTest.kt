package org.akinosoft.akinoclock.util.net

import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpUrlConnectionFetcherTest {

    private lateinit var server: HttpServer
    private val fetcher = HttpUrlConnectionFetcher(
        userAgent = "AkinoClock/test (+https://example.com/repo)",
        accept = "application/xml",
    )

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    /** Registers a static response for [path]: a fixed [status], [body] and extra [headers]. */
    private fun serve(path: String, status: Int, body: ByteArray = ByteArray(0), headers: Map<String, String> = emptyMap()) {
        server.createContext(path) { exchange ->
            headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
            if (body.isEmpty()) {
                exchange.sendResponseHeaders(status, -1)
                exchange.close()
            } else {
                exchange.sendResponseHeaders(status, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
    }

    @Test
    fun `200 with body returns Success`() = runTest {
        val body = "<rss><channel/></rss>".toByteArray()
        serve("/feed", 200, body)

        val result = fetcher.fetch(url("/feed"), null)

        assertTrue(result is FetchResult.Success)
        assertArrayEquals(body, (result as FetchResult.Success).bytes)
    }

    @Test
    fun `gzip-encoded body is decoded`() = runTest {
        val body = "<rss><channel/></rss>".toByteArray()
        val gzipped = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write(body) } }.toByteArray()
        serve("/gzip", 200, gzipped, headers = mapOf("Content-Encoding" to "gzip"))

        val result = fetcher.fetch(url("/gzip"), null)

        assertTrue(result is FetchResult.Success)
        assertArrayEquals(body, (result as FetchResult.Success).bytes)
    }

    @Test
    fun `304 returns NotModified`() = runTest {
        serve("/not-modified", 304)

        val result = fetcher.fetch(url("/not-modified"), null)

        assertEquals(FetchResult.NotModified, result)
    }

    @Test
    fun `404 returns Failure with the status code`() = runTest {
        serve("/missing", 404)

        val result = fetcher.fetch(url("/missing"), null)

        assertEquals(FetchResult.Failure(FetchResult.Failure.Reason.Http(404)), result)
    }

    @Test
    fun `500 returns Failure with the status code`() = runTest {
        serve("/error", 500)

        val result = fetcher.fetch(url("/error"), null)

        assertEquals(FetchResult.Failure(FetchResult.Failure.Reason.Http(500)), result)
    }

    @Test
    fun `connection refused returns Failure with an io reason`() = runTest {
        val closedPort = ServerSocket(0).use { it.localPort }

        val result = fetcher.fetch("http://127.0.0.1:$closedPort/", null)

        assertTrue(result is FetchResult.Failure)
        assertTrue((result as FetchResult.Failure).reason is FetchResult.Failure.Reason.Io)
    }

    @Test
    fun `body over 2MB returns Failure tooLarge`() = runTest {
        val oversized = ByteArray(2 * 1024 * 1024 + 1)
        serve("/huge", 200, oversized)

        val result = fetcher.fetch(url("/huge"), null)

        assertEquals(FetchResult.Failure(FetchResult.Failure.Reason.TooLarge), result)
    }

    @Test
    fun `If-Modified-Since header is sent only when given`() = runTest {
        var receivedWithHeader: Boolean? = null
        server.createContext("/conditional") { exchange ->
            receivedWithHeader = exchange.requestHeaders.containsKey("If-Modified-Since")
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        }

        fetcher.fetch(url("/conditional"), 1_757_000_000_000L)
        assertEquals(true, receivedWithHeader)

        fetcher.fetch(url("/conditional"), null)
        assertEquals(false, receivedWithHeader)
    }

    @Test
    fun `redirects are followed`() = runTest {
        val body = "<rss><channel/></rss>".toByteArray()
        serve("/target", 200, body)
        serve("/redirect", 302, headers = mapOf("Location" to url("/target")))

        val result = fetcher.fetch(url("/redirect"), null)

        assertTrue(result is FetchResult.Success)
        assertArrayEquals(body, (result as FetchResult.Success).bytes)
    }

    @Test
    fun `User-Agent header is present`() = runTest {
        var receivedUserAgent: String? = null
        server.createContext("/ua") { exchange ->
            receivedUserAgent = exchange.requestHeaders.getFirst("User-Agent")
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        }

        fetcher.fetch(url("/ua"), null)

        assertEquals("AkinoClock/test (+https://example.com/repo)", receivedUserAgent)
        assertFalse(receivedUserAgent.isNullOrBlank())
    }
}
