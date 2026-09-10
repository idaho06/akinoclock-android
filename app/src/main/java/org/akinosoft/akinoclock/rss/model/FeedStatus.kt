package org.akinosoft.akinoclock.rss.model

import java.time.Instant

data class FeedStatus(val lastSuccess: Instant?, val lastError: String?)
