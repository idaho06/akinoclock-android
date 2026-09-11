package org.akinosoft.akinoclock.app

import android.content.Context
import java.io.File
import java.time.Clock
import org.akinosoft.akinoclock.BuildConfig
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContentProviderCalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContextPermissionChecker
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.rss.data.DefaultRssRepository
import org.akinosoft.akinoclock.rss.data.FeedCache
import org.akinosoft.akinoclock.rss.data.HttpUrlConnectionFetcher
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.data.SharedPreferencesSettingsRepository

class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    val calendarRepository: CalendarRepository = ContentProviderCalendarRepository(context),
    val permissionChecker: PermissionChecker = ContextPermissionChecker(context),
    val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(context),
    val rssRepository: RssRepository = defaultRssRepository(context, settingsRepository, clock),
)

private fun defaultRssRepository(context: Context, settingsRepository: SettingsRepository, clock: Clock): RssRepository {
    val feedFetcher = HttpUrlConnectionFetcher(userAgent = "AkinoClock/${BuildConfig.VERSION_NAME}")
    val feedCache = FeedCache(File(context.filesDir, "rss-cache"))
    return DefaultRssRepository(settingsRepository.currentFeeds(), feedFetcher, feedCache, clock)
}
