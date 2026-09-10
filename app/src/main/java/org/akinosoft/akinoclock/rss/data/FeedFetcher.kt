package org.akinosoft.akinoclock.rss.data

interface FeedFetcher {
    suspend fun fetch(url: String, ifModifiedSinceMillis: Long?): FetchResult
}
