package org.akinosoft.akinoclock.util.net

interface HttpFetcher {
    suspend fun fetch(url: String, validators: CacheValidators?): FetchResult
}
