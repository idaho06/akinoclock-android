package org.akinosoft.akinoclock.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.ui.CalendarViewModel
import org.akinosoft.akinoclock.calendar.ui.PermissionAction
import org.akinosoft.akinoclock.calendar.ui.PermissionButtonPolicy
import org.akinosoft.akinoclock.databinding.ActivityMainBinding
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.rss.model.RssUiState
import org.akinosoft.akinoclock.rss.ui.RssViewModel

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val container get() = (application as AkinoClockApp).container

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModel.Factory(container.calendarRepository, container.permissionChecker, container.clock)
    }

    private val rssViewModel: RssViewModel by viewModels {
        RssViewModel.Factory(container.rssRepository, container.defaultFeeds, container.clock)
    }

    private var lastRssHeadlines: List<Headline>? = null

    private val requestCalendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
        updateGrantAccessButtonLabel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.calendarPanel.grantAccessButton.setOnClickListener { onGrantAccessClicked() }
        binding.rssCarousel.onHeadlineClick = { headline -> openHeadline(headline) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> binding.calendarPanel.render(state) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                rssViewModel.uiState.collect { state -> renderRss(state) }
            }
        }

        if (!container.calendarPrefs.permissionAsked()) {
            container.calendarPrefs.setPermissionAsked()
            requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.start()
        rssViewModel.start()
        updateGrantAccessButtonLabel()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
        rssViewModel.stop()
    }

    override fun onResume() {
        super.onResume()
        binding.clockView.start()
    }

    override fun onPause() {
        super.onPause()
        binding.clockView.stop()
    }

    private fun renderRss(state: RssUiState) {
        val headlines = when (state) {
            is RssUiState.Empty -> emptyList()
            is RssUiState.Showing -> state.headlines
        }
        // setHeadlines() restarts the rotation from index 0, so only call it when the list
        // actually changed — status-only emissions (e.g. a stale flag flip) must not reset
        // whichever headline is currently showing.
        if (headlines != lastRssHeadlines) {
            binding.rssCarousel.setHeadlines(headlines)
            lastRssHeadlines = headlines
        }
        binding.rssCarousel.setStale((state as? RssUiState.Showing)?.stale ?: false)
    }

    private fun openHeadline(headline: Headline) {
        val link = headline.link ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.rss_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onGrantAccessClicked() {
        when (permissionButtonAction()) {
            PermissionAction.REQUEST -> requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
            PermissionAction.OPEN_SETTINGS -> startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
            )
        }
    }

    private fun updateGrantAccessButtonLabel() {
        binding.calendarPanel.grantAccessButton.text = when (permissionButtonAction()) {
            PermissionAction.REQUEST -> getString(R.string.calendar_grant_access)
            PermissionAction.OPEN_SETTINGS -> getString(R.string.calendar_open_settings)
        }
    }

    private fun permissionButtonAction(): PermissionAction = PermissionButtonPolicy.decide(
        alreadyAsked = container.calendarPrefs.permissionAsked(),
        canShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR),
    )
}
