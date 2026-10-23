package ro.profitride.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ro.profitride.ProfitRideApp
import ro.profitride.core.AppTheme
import ro.profitride.core.DriverProfile
import ro.profitride.core.OfferStyle
import ro.profitride.core.Platform
import ro.profitride.core.ProfitCalculator
import ro.profitride.core.RideOffer
import ro.profitride.core.Themes
import ro.profitride.overlay.OverlayView

/** Oferta de exemplu din previzualizări: aceeași ca „Arată un exemplu” din Setări. */
private val SampleOffer = RideOffer(Platform.BOLT, price = 31.5, pickupKm = 2.1, tripKm = 9.4, pickupMin = 5.0, tripMin = 19.0)

/** Profil de exemplu, doar cât timp șoferul nu și-a completat costurile. Ia țara și moneda din profilul real. */
private fun sampleProfile(base: DriverProfile) = base.copy(
    consumption = 6.8, energyPrice = 7.4, yearlyFixedCosts = 3200.0, yearlyWearCosts = 5600.0,
    yearlyKm = 40_000.0, commissionBoltPct = 20.0, taxReservePct = 10.0, goodThreshold = 40.0,
)

/**
 * Cardul REAL al aplicației: aceeași clasă [OverlayView] care apare peste Bolt și Uber, la mărimea lui reală
 * (cel mult 330 dp lățime). Nu e o copie desenată separat, deci arată exact ce va vedea șoferul.
 * Cifrele sunt calculate din profilul șoferului sau, până îl completează, dintr-un profil de exemplu.
 */
@Composable
fun RealOfferCard(theme: AppTheme, style: OfferStyle, modifier: Modifier = Modifier, pill: Boolean = false) {
    val stored by ProfitRideApp.instance.profileStore.profile.collectAsState()
    val profile = remember(stored) { if (stored.isUsable) stored else sampleProfile(stored) }
    val verdict = remember(profile) { ProfitCalculator.evaluate(SampleOffer, profile) }
    val spec = Themes.spec(theme)
    AndroidView(
        modifier = modifier.fillMaxWidth().widthIn(max = 330.dp),
        factory = { ctx -> OverlayView(ctx) },
        update = { v ->
            v.applyTheme(spec)
            v.showOffer(verdict, null, profile, compact = pill, demo = false, detailed = style == OfferStyle.DETAILED)
        },
    )
}
