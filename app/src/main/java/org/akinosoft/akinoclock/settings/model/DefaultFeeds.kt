package org.akinosoft.akinoclock.settings.model

import org.akinosoft.akinoclock.rss.model.FeedConfig

/** Feeds seeded on first launch, when the `feeds` preference has never been written. */
object DefaultFeeds {
    val list: List<FeedConfig> = listOf(
        FeedConfig(url = "https://feeds.bbci.co.uk/news/world/rss.xml", title = "BBC World"),
        FeedConfig(
            url = "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/section/ultimas-noticias/portada",
            title = "El País",
        ),
        FeedConfig(url = "https://feeds.arstechnica.com/arstechnica/index", title = "Ars Technica"),
        FeedConfig(url = "https://www.phoronix.com/rss.php", title = "Phoronix"),
    )
}
