package org.akinosoft.akinoclock.rss.model

import java.time.Instant

data class Headline(
    val feedTitle: String,
    val title: String,
    val link: String?,
    val published: Instant?,
)
