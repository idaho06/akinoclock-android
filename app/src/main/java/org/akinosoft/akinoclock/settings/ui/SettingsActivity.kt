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

class SettingsActivity : ComponentActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var feedAdapter: FeedListAdapter

    private val container get() = (application as AkinoClockApp).container

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(container.settingsRepository, container.rssRepository) { mode ->
            ThemeApplier.apply(applicationContext, mode)
        }
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

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (applyingRemoteTheme) return@setOnCheckedChangeListener
            viewModel.setTheme(themeForRadioId(checkedId))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.feeds.collect(::renderFeeds) }
                launch { viewModel.themeMode.collect(::renderTheme) }
                launch { viewModel.refreshResult.collect(::showRefreshToast) }
            }
        }
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
