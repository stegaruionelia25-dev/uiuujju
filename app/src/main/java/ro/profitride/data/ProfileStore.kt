package ro.profitride.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ro.profitride.core.DriverProfile
import ro.profitride.core.OverlaySettings

/**
 * Profilul șoferului și setările overlay-ului, salvate doar pe telefon.
 * Același obiect este folosit de aplicație și de serviciul de accesibilitate.
 */
class ProfileStore(context: Context) {

    private val sp: SharedPreferences = context.getSharedPreferences("profitride", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<DriverProfile> = _profile.asStateFlow()

    private val _overlay = MutableStateFlow(loadOverlay())
    val overlay: StateFlow<OverlaySettings> = _overlay.asStateFlow()

    fun updateProfile(transform: (DriverProfile) -> DriverProfile) {
        val next = transform(_profile.value)
        if (next == _profile.value) return
        saveProfile(next)
        _profile.value = next
    }

    fun updateOverlay(transform: (OverlaySettings) -> OverlaySettings) {
        val next = transform(_overlay.value)
        if (next == _overlay.value) return
        saveOverlay(next)
        _overlay.value = next
    }

    private fun loadProfile(): DriverProfile {
        val d = DriverProfile()
        return DriverProfile(
            countryCode = sp.getString("country", null) ?: d.countryCode,
            fuelType = sp.getEnum("fuel", d.fuelType),
            consumption = sp.getD("consumption", d.consumption),
            energyPrice = sp.getD("energyPrice", d.energyPrice),
            yearlyFixedCosts = sp.getD("yearlyFixed", d.yearlyFixedCosts),
            rentMonthly = sp.getD("rentM", d.rentMonthly),
            rcaYearly = sp.getD("rcaY", d.rcaYearly),
            cascoYearly = sp.getD("cascoY", d.cascoYearly),
            accountingMonthly = sp.getD("acctM", d.accountingMonthly),
            subscriptionsMonthly = sp.getD("subsM", d.subscriptionsMonthly),
            taxesYearly = sp.getD("taxesY", d.taxesYearly),
            fleetPct = sp.getD("fleetPct", d.fleetPct),
            fleetFixedMonthly = sp.getD("fleetFixedM", d.fleetFixedMonthly),
            yearlyWearCosts = sp.getD("yearlyWear", d.yearlyWearCosts),
            yearlyKm = sp.getD("yearlyKm", d.yearlyKm),
            commissionBoltPct = sp.getD("commBolt", d.commissionBoltPct),
            commissionUberPct = sp.getD("commUber", d.commissionUberPct),
            taxReservePct = sp.getD("taxPct", d.taxReservePct),
            basis = sp.getEnum("basis", d.basis),
            goodThreshold = sp.getD("good", d.goodThreshold),
            mediumFraction = sp.getD("mediumFraction", d.mediumFraction),
            dailyTarget = sp.getD("dailyTarget", d.dailyTarget),
            onboardingDone = sp.getBoolean("onboardingDone", d.onboardingDone),
            makeName = sp.getString("make", null) ?: d.makeName,
            modelName = sp.getString("model", null) ?: d.modelName,
            year = sp.getInt("year", d.year),
        )
    }

    private fun saveProfile(p: DriverProfile) {
        sp.edit()
            .putString("country", p.countryCode)
            .putString("fuel", p.fuelType.name)
            .putD("consumption", p.consumption)
            .putD("energyPrice", p.energyPrice)
            .putD("yearlyFixed", p.yearlyFixedCosts)
            .putD("rentM", p.rentMonthly)
            .putD("rcaY", p.rcaYearly)
            .putD("cascoY", p.cascoYearly)
            .putD("acctM", p.accountingMonthly)
            .putD("subsM", p.subscriptionsMonthly)
            .putD("taxesY", p.taxesYearly)
            .putD("fleetPct", p.fleetPct)
            .putD("fleetFixedM", p.fleetFixedMonthly)
            .putD("yearlyWear", p.yearlyWearCosts)
            .putD("yearlyKm", p.yearlyKm)
            .putD("commBolt", p.commissionBoltPct)
            .putD("commUber", p.commissionUberPct)
            .putD("taxPct", p.taxReservePct)
            .putString("basis", p.basis.name)
            .putD("good", p.goodThreshold)
            .putD("mediumFraction", p.mediumFraction)
            .putD("dailyTarget", p.dailyTarget)
            .putBoolean("onboardingDone", p.onboardingDone)
            .putString("make", p.makeName)
            .putString("model", p.modelName)
            .putInt("year", p.year)
            .apply()
    }

    private fun loadOverlay(): OverlaySettings {
        val d = OverlaySettings()
        return OverlaySettings(
            yPercent = sp.getInt("ov_y", d.yPercent),
            showIdle = sp.getBoolean("ov_idle", d.showIdle),
            idleStyle = sp.getEnum("ov_style", d.idleStyle),
            compact = sp.getBoolean("ov_compact", d.compact),
            showUnsure = sp.getBoolean("ov_unsure", d.showUnsure),
            pauseAfterMin = sp.getInt("ov_pause", d.pauseAfterMin),
            idleMarkersBolt = sp.getStringSet("ov_markers_bolt", null)?.toSet() ?: d.idleMarkersBolt,
            idleMarkersUber = sp.getStringSet("ov_markers_uber", null)?.toSet() ?: d.idleMarkersUber,
            themeMode = sp.getEnum("theme_mode", d.themeMode),
            readMode = sp.getEnum("read_mode", d.readMode),
            active = sp.getBoolean("ov_active", d.active),
            resumeOnOpen = sp.getBoolean("ov_resume", d.resumeOnOpen),
        )
    }

    private fun saveOverlay(s: OverlaySettings) {
        sp.edit()
            .putInt("ov_y", s.yPercent)
            .putBoolean("ov_idle", s.showIdle)
            .putString("ov_style", s.idleStyle.name)
            .putBoolean("ov_compact", s.compact)
            .putBoolean("ov_unsure", s.showUnsure)
            .putInt("ov_pause", s.pauseAfterMin)
            .putStringSet("ov_markers_bolt", s.idleMarkersBolt)
            .putStringSet("ov_markers_uber", s.idleMarkersUber)
            .putString("theme_mode", s.themeMode.name)
            .putString("read_mode", s.readMode.name)
            .putBoolean("ov_active", s.active)
            .putBoolean("ov_resume", s.resumeOnOpen)
            .apply()
    }
}

private fun SharedPreferences.getD(key: String, default: Double): Double =
    getString(key, null)?.toDoubleOrNull() ?: default

private fun SharedPreferences.Editor.putD(key: String, value: Double): SharedPreferences.Editor =
    putString(key, value.toString())

private inline fun <reified E : Enum<E>> SharedPreferences.getEnum(key: String, default: E): E {
    val name = getString(key, null) ?: return default
    return enumValues<E>().firstOrNull { it.name == name } ?: default
}
