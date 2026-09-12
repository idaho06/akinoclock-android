package org.akinosoft.akinoclock.util.net

interface HttpFetcher {
    suspend fun fetch(url: String, ifModifiedSinceMillis: Long?): FetchResult
}
