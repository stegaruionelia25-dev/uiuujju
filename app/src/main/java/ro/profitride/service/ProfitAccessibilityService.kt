package ro.profitride.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ro.profitride.ProfitRideApp
import ro.profitride.R
import ro.profitride.core.DriverProfile
import ro.profitride.core.IdleDetector
import ro.profitride.core.LoggedRide
import ro.profitride.core.OfferParser
import ro.profitride.core.ParseResult
import ro.profitride.core.Platform
import ro.profitride.core.ProfitCalculator
import ro.profitride.core.RideOffer
import ro.profitride.core.ScreenText
import ro.profitride.core.Stats
import ro.profitride.overlay.OverlayController
import ro.profitride.overlay.OverlayText

/** Ce a citit serviciul ultima oară dintr-o aplicație; rămâne doar în memorie, pentru ecranul de calibrare. */
data class Snapshot(val texts: List<ScreenText>, val result: ParseResult, val atMillis: Long)

/**
 * Serviciul face trei lucruri și atât:
 *  1. citește textul vizibil din Bolt Driver / Uber Driver;
 *  2. calculează verdictul din profilul șoferului;
 *  3. desenează bula (care nu primește atingeri).
 * Nu apasă, nu derulează, nu acceptă și nu refuză nimic. Nu are acces la internet.
 */
class ProfitAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var overlay: OverlayController
    private val refreshRunnable = Runnable { refresh() }
    private val hidePreviewRunnable = Runnable {
        previewUntil = 0L
        overlay.hide()
    }

    private val app get() = ProfitRideApp.instance

    private var lastVerdictShownAt = 0L
    private var holdingOffer = false
    private var idleAnchor = 0L
    private var lastHiddenAt = 0L
    private var previewUntil = 0L

    private var ridesCache: List<LoggedRide> = emptyList()
    private var ridesCacheAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = OverlayController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (!::overlay.isInitialized) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (Platform.fromPackage(pkg) == null) {
            // alte aplicații: nu le citim; ne interesează doar să ascundem bula când șoferul pleacă
            if (!overlay.isVisible || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        }
        scheduleRefresh(REFRESH_DELAY_MS)
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::overlay.isInitialized) overlay.relayout(app.profileStore.overlay.value)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.destroy()
        snapshots = emptyMap()
        instance = null
    }

    // ------------------------------------------------------------------ previzualizare

    /** Arată 4 secunde o bulă de EXEMPLU, ca șoferul să-i regleze poziția din setări. */
    fun previewDemo() {
        if (!::overlay.isInitialized) return
        val settings = app.profileStore.overlay.value
        val profile = app.profileStore.profile.value.takeIf { it.isUsable } ?: demoProfile()
        val offer = RideOffer(Platform.BOLT, price = 31.5, pickupKm = 2.1, tripKm = 9.4, pickupMin = 5.0, tripMin = 19.0)
        val verdict = ProfitCalculator.evaluate(offer, profile)
        previewUntil = SystemClock.elapsedRealtime() + PREVIEW_MS
        handler.removeCallbacks(hidePreviewRunnable)
        overlay.showOffer(verdict, null, profile, settings, demo = true)
        handler.postDelayed(hidePreviewRunnable, PREVIEW_MS)
    }

    private fun demoProfile() = DriverProfile(
        consumption = 6.8, energyPrice = 7.4, yearlyFixedCosts = 3200.0, yearlyWearCosts = 5600.0,
        yearlyKm = 40_000.0, commissionBoltPct = 20.0, taxReservePct = 10.0, goodThreshold = 40.0,
    )

    // ------------------------------------------------------------------ citire și afișare

    private fun scheduleRefresh(delayMs: Long) {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, delayMs)
    }

    private fun refresh() {
        val now = SystemClock.elapsedRealtime()
        if (now < previewUntil) return
        val root = rootInActiveWindow ?: return
        val platform = Platform.fromPackage(root.packageName?.toString())
        if (platform == null) {
            hideOverlay(now)
            return
        }

        val store = app.profileStore
        val profile = store.profile.value
        val settings = store.overlay.value

        val nodes = collect(root)
        val result = OfferParser.parse(nodes)
        snapshots = snapshots + (platform to Snapshot(nodes, result, System.currentTimeMillis()))

        if (result is ParseResult.Complete) {
            if (!profile.onboardingDone || !profile.isUsable) {
                overlay.showNote(getString(R.string.ov_setup_needed), settings)
                return
            }
            val wall = System.currentTimeMillis()
            val verdict = ProfitCalculator.evaluate(result.offer.toOffer(platform), profile)
            app.rideStore.logOffer(verdict, wall)
            val rides = rides(wall)
            val avg = Stats.averagePerHour(rides, wall)
            overlay.showOffer(verdict, avg?.let { verdict.perHour - it }, profile, settings)
            lastVerdictShownAt = now
            holdingOffer = true
            idleAnchor = now
            lastHiddenAt = 0L
            return
        }

        // Oferta dispare scurt în timpul animațiilor din aplicație: o ținem 0,6 s ca să nu clipească.
        if (holdingOffer) {
            val elapsed = now - lastVerdictShownAt
            if (elapsed < HOLD_MS) {
                scheduleRefresh(HOLD_MS - elapsed + 50)
                return
            }
            holdingOffer = false
        }

        val idle = IdleDetector.isIdle(nodes.map { it.text }, settings.markersFor(platform))
        when {
            idle && settings.showIdle -> {
                if (!profile.onboardingDone || !profile.isUsable) {
                    overlay.showNote(getString(R.string.ov_setup_needed), settings)
                    return
                }
                if (idleAnchor == 0L || (lastHiddenAt != 0L && now - lastHiddenAt > RESET_AFTER_MS)) idleAnchor = now
                lastHiddenAt = 0L
                val wall = System.currentTimeMillis()
                val rides = rides(wall)
                val minutes = ((now - idleAnchor) / 60_000L).toInt()
                val pause = minutes.takeIf { it >= settings.pauseAfterMin }
                val messages = Stats
                    .idleMessages(profile, Stats.today(rides, wall), Stats.bestWindowStartHour(rides))
                    .map { OverlayText.idleMessage(this, it, profile) }
                overlay.showIdle(messages, pause, settings)
                scheduleRefresh(IDLE_TICK_MS)
            }
            (result is ParseResult.Incomplete || result is ParseResult.Unsure) && settings.showUnsure ->
                overlay.showNote(getString(R.string.ov_unsure), settings)
            else -> hideOverlay(now)
        }
    }

    private fun hideOverlay(now: Long) {
        if (overlay.isVisible) overlay.hide()
        holdingOffer = false
        if (lastHiddenAt == 0L) lastHiddenAt = now
    }

    private fun rides(wallMillis: Long): List<LoggedRide> {
        if (wallMillis - ridesCacheAt > RIDES_CACHE_MS) {
            ridesCache = app.rideStore.acceptedRides(wallMillis - THIRTY_DAYS_MS)
            ridesCacheAt = wallMillis
        }
        return ridesCache
    }

    /**
     * Strânge textele vizibile (cel mult 600 de noduri). Câmpurile editabile și cele de parolă sunt sărite.
     * Textele nu sunt salvate nicăieri; din ele se extrag doar numerele cursei.
     */
    private fun collect(root: AccessibilityNodeInfo): List<ScreenText> {
        val out = ArrayList<ScreenText>(64)
        val rect = Rect()
        var budget = MAX_NODES

        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || budget-- <= 0 || depth > MAX_DEPTH) return
            if (!node.isVisibleToUser) return
            if (!node.isPassword && !node.isEditable) {
                val text = (node.text ?: node.contentDescription)?.toString()?.trim()
                if (!text.isNullOrEmpty() && text.length <= MAX_TEXT) {
                    node.getBoundsInScreen(rect)
                    out.add(ScreenText(text, rect.top, rect.left))
                }
            }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }

        walk(root, 0)
        return out
    }

    companion object {
        private const val REFRESH_DELAY_MS = 120L
        private const val HOLD_MS = 600L
        private const val IDLE_TICK_MS = 30_000L
        private const val RESET_AFTER_MS = 120_000L
        private const val RIDES_CACHE_MS = 20_000L
        private const val THIRTY_DAYS_MS = 30L * 24 * 3_600_000
        private const val PREVIEW_MS = 4_500L
        private const val MAX_NODES = 600
        private const val MAX_DEPTH = 40
        private const val MAX_TEXT = 160

        @Volatile
        var instance: ProfitAccessibilityService? = null
            private set

        /** Ultima citire per aplicație, doar în memorie. Folosită de ecranul de calibrare. */
        @Volatile
        var snapshots: Map<Platform, Snapshot> = emptyMap()
            private set
    }
}
