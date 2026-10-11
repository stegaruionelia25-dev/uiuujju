package ro.profitride.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.profitride.ProfitRideApp
import ro.profitride.core.DayStats
import ro.profitride.core.DriverProfile
import ro.profitride.core.OverlaySettings
import ro.profitride.core.Stats
import ro.profitride.data.RideRow
import ro.profitride.service.ProfitAccessibilityService

data class HomeStats(
    val today: DayStats = DayStats(0.0, 0, null),
    val offersToday: Int = 0,
    val avgPerHour: Double? = null,
    val bestHour: Int? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ProfitRideApp

    val profile: StateFlow<DriverProfile> = app.profileStore.profile
    val overlay: StateFlow<OverlaySettings> = app.profileStore.overlay

    private val _serviceEnabled = MutableStateFlow(false)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    private val _rides = MutableStateFlow<List<RideRow>>(emptyList())
    val rides: StateFlow<List<RideRow>> = _rides.asStateFlow()

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    fun updateProfile(transform: (DriverProfile) -> DriverProfile) = app.profileStore.updateProfile(transform)

    fun updateOverlay(transform: (OverlaySettings) -> OverlaySettings) = app.profileStore.updateOverlay(transform)

    /** Se apelează la fiecare revenire în aplicație. */
    fun refresh() {
        _serviceEnabled.value = AccessibilityStatus.isEnabled(app)
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val recent = app.rideStore.recent(200)
            val accepted = app.rideStore.acceptedRides(now - THIRTY_DAYS_MS)
            val today = Stats.today(accepted, now)
            val workDayStart = now - 18 * 3_600_000L
            _rides.value = recent
            _stats.value = HomeStats(
                today = today,
                offersToday = recent.count { it.timestamp >= workDayStart },
                avgPerHour = Stats.averagePerHour(accepted, now),
                bestHour = Stats.bestWindowStartHour(accepted),
            )
        }
    }

    fun setAccepted(id: Long, accepted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.setAccepted(id, accepted)
            refresh()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.clear()
            refresh()
        }
    }

    /** Arată scurt o bulă de EXEMPLU, ca șoferul să-i regleze poziția. Fără efect dacă serviciul e oprit. */
    fun previewOverlay() {
        ProfitAccessibilityService.instance?.previewDemo()
    }

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24 * 3_600_000
    }
}

object AccessibilityStatus {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(context, ProfitAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Pe Android 13+, aplicațiile instalate din afara magazinului au setări restricționate; se permit de aici. */
    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
