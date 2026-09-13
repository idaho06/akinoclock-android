package org.akinosoft.akinoclock.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.calendar.ui.CalendarViewModel
import org.akinosoft.akinoclock.calendar.ui.PermissionAction
import org.akinosoft.akinoclock.calendar.ui.PermissionButtonPolicy
import org.akinosoft.akinoclock.databinding.ActivityMainBinding
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.rss.ui.RssViewModel
import org.akinosoft.akinoclock.settings.ui.SettingsActivity
import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.akinosoft.akinoclock.weather.ui.WeatherForecastUrl
import org.akinosoft.akinoclock.weather.ui.WeatherViewModel

class MainActivity : ComponentActivity() {

    internal lateinit var binding: ActivityMainBinding

    private val container get() = (application as AkinoClockApp).container

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModel.Factory(container.calendarRepository, container.permissionChecker, container.clock)
    }

    private val rssViewModel: RssViewModel by viewModels {
        RssViewModel.Factory(container.rssRepository, container.settingsRepository.feeds, container.clock)
    }

    private val weatherViewModel: WeatherViewModel by viewModels {
        WeatherViewModel.Factory(container.weatherRepository, container.settingsRepository.weatherLocation, container.clock)
    }

    private val requestCalendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
        updateGrantAccessButtonLabel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.calendarPanel.grantAccessButton.setOnClickListener { onGrantAccessClicked() }
        binding.rssCarousel.onHeadlineClick = { headline -> openHeadline(headline) }
        binding.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.weatherStrip.onPlaceholderClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.weatherStrip.onForecastClick = { location -> openForecast(location) }
        binding.calendarPanel.monthGridView.onDateClick = { date -> openCalendarDate(date) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> binding.calendarPanel.render(state) } }
                launch { rssViewModel.uiState.collect { state -> binding.rssCarousel.render(state) } }
                launch { container.nextAlarmSource.changes().collect { binding.clockView.nextAlarm = it } }
                launch { weatherViewModel.uiState.collect { state -> binding.weatherStrip.render(state) } }
            }
        }

        if (!container.settingsRepository.permissionAsked()) {
            container.settingsRepository.setPermissionAsked()
            requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.start()
        rssViewModel.start()
        weatherViewModel.start()
        updateGrantAccessButtonLabel()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
        rssViewModel.stop()
        weatherViewModel.stop()
    }

    override fun onResume() {
        super.onResume()
        binding.clockView.start()
    }

    override fun onPause() {
        super.onPause()
        binding.clockView.stop()
    }

    private fun openHeadline(headline: Headline) {
        val link = headline.link ?: return
        openExternally(Uri.parse(link), R.string.rss_no_browser)
    }

    private fun openForecast(location: WeatherLocation) {
        openExternally(Uri.parse(WeatherForecastUrl.build(location)), R.string.rss_no_browser)
    }

    private fun openCalendarDate(date: LocalDate) {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(millis.toString()).build()
        openExternally(uri, R.string.calendar_no_app)
    }

    private fun openExternally(uri: Uri, noAppMessage: Int) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, noAppMessage, Toast.LENGTH_SHORT).show()
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
        alreadyAsked = container.settingsRepository.permissionAsked(),
        canShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR),
    )
}
