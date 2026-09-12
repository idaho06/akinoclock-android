package org.akinosoft.akinoclock.app

import android.content.Context
import java.io.File
import java.time.Clock
import org.akinosoft.akinoclock.BuildConfig
import org.akinosoft.akinoclock.calendar.data.CalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContentProviderCalendarRepository
import org.akinosoft.akinoclock.calendar.data.ContextPermissionChecker
import org.akinosoft.akinoclock.calendar.data.PermissionChecker
import org.akinosoft.akinoclock.clock.alarm.AlarmManagerNextAlarmSource
import org.akinosoft.akinoclock.clock.alarm.NextAlarmSource
import org.akinosoft.akinoclock.rss.data.DefaultRssRepository
import org.akinosoft.akinoclock.rss.data.RssRepository
import org.akinosoft.akinoclock.settings.data.SettingsRepository
import org.akinosoft.akinoclock.settings.data.SharedPreferencesSettingsRepository
import org.akinosoft.akinoclock.util.FeedCache
import org.akinosoft.akinoclock.util.net.HttpUrlConnectionFetcher
import org.akinosoft.akinoclock.weather.data.DefaultWeatherRepository
import org.akinosoft.akinoclock.weather.data.GeocodingClient
import org.akinosoft.akinoclock.weather.data.OpenMeteoGeocodingClient
import org.akinosoft.akinoclock.weather.data.WeatherRepository

private const val RSS_ACCEPT_HEADER = "application/rss+xml, application/atom+xml, application/xml, text/xml, */*"

class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    val calendarRepository: CalendarRepository = ContentProviderCalendarRepository(context),
    val permissionChecker: PermissionChecker = ContextPermissionChecker(context),
    val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(context),
    val rssRepository: RssRepository = run {
        val feedFetcher =
            HttpUrlConnectionFetcher(userAgent = "AkinoClock/${BuildConfig.VERSION_NAME}", accept = RSS_ACCEPT_HEADER)
        val feedCache = FeedCache(File(context.filesDir, "rss-cache"))
        DefaultRssRepository(settingsRepository.currentFeeds(), feedFetcher, feedCache, clock)
    },
    val nextAlarmSource: NextAlarmSource = AlarmManagerNextAlarmSource(context),
    val weatherRepository: WeatherRepository = run {
        val weatherFetcher =
            HttpUrlConnectionFetcher(userAgent = "AkinoClock/${BuildConfig.VERSION_NAME}", accept = "application/json")
        val weatherCache = FeedCache(File(context.filesDir, "weather-cache"))
        DefaultWeatherRepository(weatherFetcher, weatherCache, clock)
    },
    val geocodingClient: GeocodingClient = OpenMeteoGeocodingClient(
        HttpUrlConnectionFetcher(userAgent = "AkinoClock/${BuildConfig.VERSION_NAME}", accept = "application/json"),
    ),
)
