package ro.profitride.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ro.profitride.ProfitRideApp
import ro.profitride.R
import ro.profitride.core.DriverProfile
import ro.profitride.core.IdleDetector
import ro.profitride.core.LoggedRide
import ro.profitride.core.OfferParser
import ro.profitride.core.ParseResult
import ro.profitride.core.Platform
import ro.profitride.core.ProfitCalculator
import ro.profitride.core.ReadMode
import ro.profitride.core.RideOffer
import ro.profitride.core.ScreenText
import ro.profitride.core.Stats
import ro.profitride.overlay.OverlayController
import ro.profitride.overlay.OverlayText

/** Ce a citit serviciul ultima oară dintr-o aplicație; rămâne doar în memorie, pentru ecranul de calibrare. */
data class Snapshot(val texts: List<ScreenText>, val result: ParseResult, val atMillis: Long, val visual: Boolean = false)

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

    private var visualBusy = false
    private var testUntil = 0L
    private val testEndRunnable = Runnable { refresh() }
    private var lastShotAt = 0L
    private val recognizerDelegate = lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val recognizer by recognizerDelegate

    private var ridesCache: List<LoggedRide> = emptyList()
    private var ridesCacheAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = OverlayController(this)
        StatusNotification.show(this, app.profileStore.overlay.value.active)
    }

    /** Apelat când șoferul pornește/oprește ProfitRide (din aplicație sau din notificare). */
    private fun onActiveChanged() {
        val active = app.profileStore.overlay.value.active
        StatusNotification.show(this, active)
        if (!active) {
            handler.removeCallbacks(refreshRunnable)
            previewUntil = 0L
            testUntil = 0L
            hideOverlay(SystemClock.elapsedRealtime())
        } else {
            scheduleRefresh(REFRESH_DELAY_MS)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (!::overlay.isInitialized) return
        if (!app.profileStore.overlay.value.active) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (Platform.fromPackage(pkg) == null && !testActive() && !isGallery(pkg)) {
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
        if (recognizerDelegate.isInitialized()) recognizer.close()
        if (::overlay.isInitialized) overlay.destroy()
        snapshots = emptyMap()
        offerSnapshots = emptyMap()
        StatusNotification.cancel(this)
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
        if (!app.profileStore.overlay.value.active) {
            hideOverlay(now)
            return
        }
        if (now < previewUntil) return
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString()
        val platform = Platform.fromPackage(pkg)
        // Test: citește vizual orice ecran (de ex. o poză cu o ofertă din galerie), dar nu propria aplicație.
        // Galeria foto: o poză cu o ofertă Bolt deschisă acolo se citește vizual, fără să apeși „test”.
        val testing = platform == null && pkg != packageName && (testActive() || isGallery(pkg))
        if (platform == null && !testing) {
            hideOverlay(now)
            return
        }

        val settings = app.profileStore.overlay.value
        val nodes = collect(root)
        val canSeeScreen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val wantVisual = canSeeScreen && (testing || when (settings.readMode) {
            ReadMode.VISUAL -> true
            ReadMode.AUTO -> nodes.size < MIN_TEXT_NODES
            ReadMode.TEXT -> false
        })
        if (wantVisual) {
            requestVisual(platform, nodes, testing)
            return
        }
        handleNodes(platform ?: return, nodes, visual = false, now = now)
    }

    private fun isGallery(pkg: String?): Boolean {
        val p = pkg?.lowercase() ?: return false
        return GALLERY_HINTS.any { p.contains(it) }
    }

    private fun testActive() = SystemClock.elapsedRealtime() < testUntil

    /** Timp de 5 minute citește vizual orice ecran, ca să poți încerca aplicația cu o poză cu o ofertă. */
    fun startTest() {
        testUntil = SystemClock.elapsedRealtime() + TEST_MS
        handler.removeCallbacks(testEndRunnable)
        handler.postDelayed(testEndRunnable, TEST_MS + 300)
        scheduleRefresh(300)
    }

    private fun guessPlatform(lines: List<ScreenText>): Platform =
        if (lines.any { it.text.contains("uber", ignoreCase = true) }) Platform.UBER else Platform.BOLT

    /**
     * Citirea vizuală: o captură a ecranului (permisă de serviciul de accesibilitate, fără alte permisiuni),
     * transformată în text pe telefon. Captura nu se salvează și nu pleacă nicăieri.
     */
    @android.annotation.SuppressLint("NewApi")
    private fun requestVisual(platform: Platform?, fallback: List<ScreenText>, testing: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (visualBusy) return
        val wait = SHOT_MIN_INTERVAL_MS - (now - lastShotAt)
        if (wait > 0) {
            scheduleRefresh(wait + 30)
            return
        }
        visualBusy = true
        lastShotAt = now
        val failed = {
            visualBusy = false
            handleNodes(platform ?: guessPlatform(fallback), fallback, visual = false, now = SystemClock.elapsedRealtime(), testing = testing)
        }
        takeScreenshot(
            android.view.Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                    val soft = hardware?.copy(Bitmap.Config.ARGB_8888, false)
                    hardware?.recycle()
                    buffer.close()
                    if (soft == null) {
                        failed()
                        return
                    }
                    val scale = if (soft.width > OCR_WIDTH) OCR_WIDTH.toFloat() / soft.width else 1f
                    val small = if (scale < 1f) {
                        Bitmap.createScaledBitmap(soft, OCR_WIDTH, (soft.height * scale).toInt(), true)
                    } else {
                        soft
                    }
                    val ownBounds = overlay.bounds()
                    recognizer.process(InputImage.fromBitmap(small, 0))
                        .addOnSuccessListener(mainExecutor) { text ->
                            val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                                val box = line.boundingBox ?: return@mapNotNull null
                                val cx = (box.centerX() / scale).toInt()
                                val cy = (box.centerY() / scale).toInt()
                                if (ownBounds != null && ownBounds.contains(cx, cy)) return@mapNotNull null
                                ScreenText(line.text, (box.top / scale).toInt(), (box.left / scale).toInt())
                            }
                            if (small !== soft) small.recycle()
                            soft.recycle()
                            visualBusy = false
                            val seen = platform ?: guessPlatform(lines)
                            if (lines.isEmpty()) {
                                handleNodes(platform ?: seen, fallback, visual = false, now = SystemClock.elapsedRealtime(), testing = testing)
                            } else {
                                handleNodes(seen, lines, visual = true, now = SystemClock.elapsedRealtime(), testing = testing)
                            }
                            scheduleRefresh(VISUAL_INTERVAL_MS)
                        }
                        .addOnFailureListener(mainExecutor) {
                            soft.recycle()
                            failed()
                        }
                }

                override fun onFailure(errorCode: Int) {
                    failed()
                }
            },
        )
    }

    private fun handleNodes(platform: Platform, nodes: List<ScreenText>, visual: Boolean, now: Long, testing: Boolean = false) {
        val store = app.profileStore
        val profile = store.profile.value
        val settings = store.overlay.value

        val result = OfferParser.parse(nodes)
        val snap = Snapshot(nodes, result, System.currentTimeMillis(), visual)
        snapshots = snapshots + (platform to snap)
        if (result !is ParseResult.NoOffer) offerSnapshots = offerSnapshots + (platform to snap)

        if (result is ParseResult.Complete) {
            if (!profile.onboardingDone || !profile.isUsable) {
                overlay.showNote(getString(R.string.ov_setup_needed), settings)
                return
            }
            val wall = System.currentTimeMillis()
            val verdict = ProfitCalculator.evaluate(result.offer.toOffer(platform), profile)
            if (!testing) app.rideStore.logOffer(verdict, wall)
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
        val offlineHome = IdleDetector.isOfflineHome(nodes.map { it.text })
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
            offlineHome && settings.showIdle -> {
                if (!profile.onboardingDone || !profile.isUsable) {
                    overlay.showNote(getString(R.string.ov_setup_needed), settings)
                } else {
                    overlay.showNote(getString(R.string.ov_ready), settings)
                }
                lastHiddenAt = 0L
                idleAnchor = 0L
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
        /** Pornește/oprește ProfitRide; singura sursă de adevăr e setarea salvată. */
        /** Redesenează notificarea (de ex. după ce șoferul a permis notificările). */
        fun refreshNotification() {
            instance?.let { StatusNotification.show(it, ProfitRideApp.instance.profileStore.overlay.value.active) }
        }

        fun setActive(active: Boolean) {
            ProfitRideApp.instance.profileStore.updateOverlay { it.copy(active = active) }
            instance?.onActiveChanged()
        }

        private const val REFRESH_DELAY_MS = 120L
        private const val HOLD_MS = 600L
        private const val IDLE_TICK_MS = 30_000L
        private const val RESET_AFTER_MS = 120_000L
        private const val RIDES_CACHE_MS = 20_000L
        private const val THIRTY_DAYS_MS = 30L * 24 * 3_600_000
        private const val PREVIEW_MS = 4_500L
        private const val MAX_NODES = 600
        private val GALLERY_HINTS = listOf("gallery", "photos", "photo", "camera")
        private const val MAX_DEPTH = 40
        private const val MAX_TEXT = 160
        private const val MIN_TEXT_NODES = 4
        private const val SHOT_MIN_INTERVAL_MS = 1_000L
        private const val VISUAL_INTERVAL_MS = 1_200L
        private const val OCR_WIDTH = 1080
        private const val TEST_MS = 5 * 60_000L

        @Volatile
        var instance: ProfitAccessibilityService? = null
            private set

        /** Ultima citire per aplicație, doar în memorie. Folosită de ecranul de calibrare. */
        @Volatile
        var snapshots: Map<Platform, Snapshot> = emptyMap()

        /** ultimul ecran care a părut o ofertă, ca să-l poți vedea în Calibrare după ce dispare */
        @Volatile
        var offerSnapshots: Map<Platform, Snapshot> = emptyMap()
            private set
    }
}
