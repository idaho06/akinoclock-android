package org.akinosoft.akinoclock.util.net

/**
 * Raw `ETag` and `Last-Modified` response header values, sent back unchanged as `If-None-Match` /
 * `If-Modified-Since` so the server can answer 304 by exact match.
 */
data class CacheValidators(val etag: String?, val lastModified: String?)
