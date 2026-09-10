package org.akinosoft.akinoclock.rss.data

import kotlinx.coroutines.flow.Flow
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.rss.model.FeedStatus
import org.akinosoft.akinoclock.rss.model.Headline

interface RssRepository {
    fun headlines(): Flow<List<Headline>>
    fun status(): Flow<Map<String, FeedStatus>>
    suspend fun refresh(feeds: List<FeedConfig>): RefreshOutcome
}
