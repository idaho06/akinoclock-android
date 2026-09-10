package org.akinosoft.akinoclock.app

import android.content.Context
import java.io.File
import java.time.Clock
import org.akinosoft.akinoclock.BuildConfig
import org.akinosoft.akinoclock.calendar.data.CalendarPrefs
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContentProviderCalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContextPermissionChecker
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.calendar.data.SharedPreferencesCalendarPrefs
import org.akinosoft.akinoclock.rss.data.DefaultRssRepository
import org.akinosoft.akinoclock.rss.data.FeedCache
import org.akinosoft.akinoclock.rss.data.HttpUrlConnectionFetcher
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.rss.model.FeedConfig

class AppContainer(private val context: Context) {

    val clock: Clock = Clock.systemDefaultZone()

    val calendarRepository: CalendarRepository = ContentProviderCalendarRepository(context)
    val permissionChecker: PermissionChecker = ContextPermissionChecker(context)
    val calendarPrefs: CalendarPrefs = SharedPreferencesCalendarPrefs(context)

    // Placeholder until phase 05 adds a settings-backed feed list: the user hasn't picked real
    // feeds yet, so this ships a single well-known, stable public feed rather than an empty list.
    val defaultFeeds: List<FeedConfig> = listOf(
        FeedConfig(url = "https://feeds.bbci.co.uk/news/world/rss.xml", title = "BBC World"),
    )

    private val feedFetcher = HttpUrlConnectionFetcher(userAgent = "AkinoClock/${BuildConfig.VERSION_NAME}")
    private val feedCache = FeedCache(File(context.filesDir, "rss-cache"))
    val rssRepository: RssRepository = DefaultRssRepository(defaultFeeds, feedFetcher, feedCache, clock)
}
