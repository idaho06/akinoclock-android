package org.akinosoft.akinoclock.rss.model

sealed class RssUiState {
    data class Empty(val noFeeds: Boolean) : RssUiState()
    data class Showing(val headlines: List<Headline>, val stale: Boolean) : RssUiState()
}
