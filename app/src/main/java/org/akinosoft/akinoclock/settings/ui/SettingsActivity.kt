package org.akinosoft.akinoclock.settings.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.app.AkinoClockApp
import org.akinosoft.akinoclock.app.ThemeApplier
import org.akinosoft.akinoclock.databinding.ActivitySettingsBinding
import org.akinosoft.akinoclock.rss.data.RefreshOutcome
import org.akinosoft.akinoclock.rss.model.FeedConfig
import org.akinosoft.akinoclock.settings.model.ThemeMode
import org.akinosoft.akinoclock.weather.data.GeocodingResult
import org.akinosoft.akinoclock.weather.model.WeatherLocation

class SettingsActivity : ComponentActivity() {

    internal lateinit var binding: ActivitySettingsBinding
    private lateinit var feedAdapter: FeedListAdapter
    private var locationSearchDialog: android.app.AlertDialog? = null

    private val container get() = (application as AkinoClockApp).container

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(
            container.settingsRepository,
            container.rssRepository,
            container.weatherRepository,
            container.geocodingClient,
        ) { mode -> ThemeApplier.apply(applicationContext, mode) }
    }

    /** Set while [renderTheme] is applying a remote state change, to keep it from re-triggering
     * [SettingsViewModel.setTheme] via the RadioGroup's own listener. */
    private var applyingRemoteTheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        feedAdapter = FeedListAdapter(this) { feed -> viewModel.removeFeed(feed.url) }
        binding.feedListView.adapter = feedAdapter
        binding.feedListView.setOnItemClickListener { _, _, position, _ ->
            val feed = feedAdapter.getItem(position) ?: return@setOnItemClickListener
            FeedEditDialog.show(this, feed.url) { newUrl -> viewModel.updateFeed(feed.url, newUrl) }
        }

        binding.addFeedButton.setOnClickListener {
            FeedEditDialog.show(this, existingUrl = null) { url -> viewModel.addFeed(url) }
        }

        binding.refreshNowButton.setOnClickListener { viewModel.refreshNow() }

        binding.changeWeatherLocationButton.setOnClickListener {
            locationSearchDialog = LocationSearchDialog.show(this) { query -> viewModel.searchLocation(query) }
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (applyingRemoteTheme) return@setOnCheckedChangeListener
            viewModel.setTheme(themeForRadioId(checkedId))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.feeds.collect(::renderFeeds) }
                launch { viewModel.themeMode.collect(::renderTheme) }
                launch { viewModel.refreshResult.collect(::showRefreshToast) }
                launch { viewModel.weatherLocation.collect(::renderWeatherLocation) }
                launch { viewModel.searchResult.collect(::handleSearchResult) }
            }
        }
    }

    override fun onDestroy() {
        locationSearchDialog?.dismiss()
        super.onDestroy()
    }

    private fun renderFeeds(feeds: List<FeedConfig>) {
        feedAdapter.clear()
        feedAdapter.addAll(feeds)
    }

    private fun renderTheme(mode: ThemeMode) {
        val id = radioIdForTheme(mode)
        if (binding.themeRadioGroup.checkedRadioButtonId != id) {
            applyingRemoteTheme = true
            binding.themeRadioGroup.check(id)
            applyingRemoteTheme = false
        }
    }

    private fun themeForRadioId(id: Int): ThemeMode = when (id) {
        R.id.themeLightRadio -> ThemeMode.LIGHT
        R.id.themeDarkRadio -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    private fun radioIdForTheme(mode: ThemeMode): Int = when (mode) {
        ThemeMode.SYSTEM -> R.id.themeSystemRadio
        ThemeMode.LIGHT -> R.id.themeLightRadio
        ThemeMode.DARK -> R.id.themeDarkRadio
    }

    private fun renderWeatherLocation(location: WeatherLocation?) {
        binding.weatherLocationLabel.text = location?.name ?: getString(R.string.settings_weather_location_not_set)
    }

    private fun handleSearchResult(result: GeocodingResult) {
        val dialog = locationSearchDialog ?: return
        when (result) {
            is GeocodingResult.Found -> LocationSearchDialog.showResults(this, dialog, result.locations) { location ->
                viewModel.setWeatherLocation(location)
            }
            GeocodingResult.NoResults ->
                LocationSearchDialog.showInlineError(dialog, getString(R.string.settings_weather_location_no_results))
            GeocodingResult.Failed ->
                LocationSearchDialog.showInlineError(dialog, getString(R.string.settings_weather_location_search_failed))
        }
    }

    private fun showRefreshToast(outcome: RefreshOutcome) {
        val messageRes = when (outcome) {
            RefreshOutcome.SUCCESS -> R.string.settings_refresh_updated
            RefreshOutcome.PARTIAL_FAILURE -> R.string.settings_refresh_some_failed
            RefreshOutcome.ALL_FAILED -> R.string.settings_refresh_all_failed
            RefreshOutcome.NO_FEEDS -> R.string.settings_refresh_no_feeds
        }
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }
}
