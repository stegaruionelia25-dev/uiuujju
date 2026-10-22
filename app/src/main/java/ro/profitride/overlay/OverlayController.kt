package ro.profitride.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import kotlin.math.min
import kotlin.math.roundToInt
import ro.profitride.ProfitRideApp
import ro.profitride.core.BubbleScale
import ro.profitride.core.DriverProfile
import ro.profitride.core.OfferStyle
import ro.profitride.core.OverlaySettings
import ro.profitride.core.ThemeSpec
import ro.profitride.core.Themes
import ro.profitride.core.Verdict

/**
 * Pune bula într-o fereastră de tip accesibilitate. Fereastra nu poate lua focus. Implicit nu primește atingeri,
 * deci tot ce atinge șoferul ajunge direct în Bolt sau Uber. Doar dacă șoferul a lăsat activ zoomul cu două degete,
 * fereastra verdictului prinde atingerile, dar numai pe suprafața bulei (fereastra are exact mărimea bulei).
 * Poziția pe verticală vine din setări (procent din înălțimea ecranului), iar lățimea se adaptează la ecran.
 */
class OverlayController(private val service: AccessibilityService) {

    private enum class Kind { OFFER, DETAIL, COMPACT, IDLE, NOTE }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: OverlayView? = null
    private var added = false
    private var lastKind = Kind.IDLE
    private var lastSettings = OverlaySettings()

    val isVisible: Boolean get() = added

    /** Dreptunghiul bulei pe ecran; folosit ca să nu citim vizual propriul text al bulei. */
    fun bounds(): android.graphics.Rect? {
        val v = view ?: return null
        if (!added || v.width == 0) return null
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return android.graphics.Rect(loc[0], loc[1], loc[0] + v.width, loc[1] + v.height)
    }

    fun showOffer(
        verdict: Verdict,
        vsAverage: Double?,
        profile: DriverProfile,
        settings: OverlaySettings,
        demo: Boolean = false,
    ) {
        val compact = settings.compact || isSmallScreen()
        val detailed = !compact && settings.offerStyle == OfferStyle.DETAILED
        place(
            when {
                compact -> Kind.COMPACT
                detailed -> Kind.DETAIL
                else -> Kind.OFFER
            },
            settings,
        )
        view?.showOffer(verdict, vsAverage, profile, compact, demo, detailed)
    }

    fun showIdle(messages: List<String>, pauseMinutes: Int?, settings: OverlaySettings) {
        place(Kind.IDLE, settings)
        view?.showIdle(messages, pauseMinutes, settings.idleStyle)
    }

    fun showNote(text: String, settings: OverlaySettings) {
        place(Kind.NOTE, settings)
        view?.showNote(text)
    }

    fun hide() {
        val v = view ?: return
        v.stopAnimations()
        if (added) {
            runCatching { windowManager.removeViewImmediate(v) }
            added = false
        }
    }

    fun destroy() {
        hide()
        view = null
    }

    /** La rotirea ecranului sau când șoferul mută poziția din setări. */
    fun relayout(newSettings: OverlaySettings) {
        val settings = withLive(newSettings)
        lastSettings = settings
        val v = view ?: return
        configure(v, lastKind, settings)
        if (added) runCatching { windowManager.updateViewLayout(v, layoutParams(lastKind, settings)) }
    }

    /** Tema, mărimea și zoomul bulei, după setările curente. */
    private fun configure(v: OverlayView, kind: Kind, settings: OverlaySettings) {
        v.applyTheme(themeSpec(settings))
        val scalable = kind == Kind.OFFER || kind == Kind.DETAIL || kind == Kind.COMPACT
        v.setScaleState(if (scalable) effectiveScale(kind, settings.bubbleScale) else 1f, settings.bubbleScale)
        v.zoomEnabled = scalable && settings.bubbleZoom
        v.onPinch = { pct -> onPinchLive(pct) }
        v.onPinchEnd = { pct -> onPinchDone(pct) }
    }

    private fun themeSpec(settings: OverlaySettings): ThemeSpec {
        val night = (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return Themes.spec(Themes.effective(settings.appTheme, settings.autoTheme, night))
    }

    /** Mărimea din timpul gestului: o ținem aici ca un eveniment din Bolt/Uber să n-o reseteze la cea salvată. */
    private var livePct: Int? = null

    private fun withLive(s: OverlaySettings): OverlaySettings = livePct?.let { s.copy(bubbleScale = it) } ?: s

    /** În timpul gestului: bula se mărește pe loc, fără să salvăm nimic. */
    private fun onPinchLive(percent: Int) {
        val v = view ?: return
        val eff = effectiveScale(lastKind, percent)
        // dacă ecranul nu mai are loc, oprim valoarea dorită la limită, ca să se poată micșora imediat
        val shown = minOf(percent, (eff * 100f).roundToInt()).coerceAtLeast(BubbleScale.MIN)
        v.setScaleState(eff, shown)
        livePct = shown
        val s = lastSettings.copy(bubbleScale = shown)
        if (added) runCatching { windowManager.updateViewLayout(v, layoutParams(lastKind, s)) }
    }

    /** La ridicarea degetelor: salvăm mărimea, ca să rămână și la următoarea ofertă. */
    private fun onPinchDone(percent: Int) {
        val eff = effectiveScale(lastKind, percent)
        val shown = minOf(percent, (eff * 100f).roundToInt()).coerceAtLeast(BubbleScale.MIN)
        ProfitRideApp.instance.profileStore.updateOverlay { it.copy(bubbleScale = BubbleScale.clamp(shown)) }
        livePct = null
    }

    /** Lățimea normală a bulei (fără zoom), în pixeli. */
    private fun baseWidthPx(): Int {
        val dm = service.resources.displayMetrics
        val margin = (16 * dm.density).roundToInt()
        return min(dm.widthPixels - 2 * margin, (MAX_PANEL_DP * dm.density).roundToInt())
    }

    /** Factorul real: limitat de lățimea ecranului și de zona de deasupra butoanelor Acceptă / Refuză. */
    private fun effectiveScale(kind: Kind, percent: Int): Float {
        val dm = service.resources.displayMetrics
        val margin = (16 * dm.density).roundToInt()
        val base = baseWidthPx().coerceAtLeast(1)
        val widthCap = (dm.widthPixels - 2 * margin).toFloat() / base
        val y = dm.heightPixels * lastSettings.yPercent.coerceIn(5, 80) / 100f
        val baseHeight = when (kind) {
            Kind.OFFER -> OFFER_HEIGHT_DP
            Kind.DETAIL -> DETAIL_HEIGHT_DP
            else -> COMPACT_HEIGHT_DP
        } * dm.density
        val heightCap = (dm.heightPixels * SAFE_BOTTOM - y) / baseHeight
        return BubbleScale.effective(percent, widthCap, heightCap)
    }

    private fun place(kind: Kind, newSettings: OverlaySettings) {
        val settings = withLive(newSettings)
        lastKind = kind
        lastSettings = settings
        val v = view ?: OverlayView(service).also { view = it }
        configure(v, kind, settings)
        val params = layoutParams(kind, settings)
        if (!added) {
            runCatching { windowManager.addView(v, params) }.onSuccess { added = true }
        } else {
            runCatching { windowManager.updateViewLayout(v, params) }
        }
    }

    private fun isSmallScreen(): Boolean {
        val dm = service.resources.displayMetrics
        return dm.widthPixels / dm.density < SMALL_SCREEN_DP
    }

    private fun layoutParams(kind: Kind, settings: OverlaySettings): WindowManager.LayoutParams {
        val dm = service.resources.displayMetrics
        val width = when (kind) {
            Kind.OFFER, Kind.DETAIL -> (baseWidthPx() * effectiveScale(kind, settings.bubbleScale)).roundToInt()
            else -> WindowManager.LayoutParams.WRAP_CONTENT
        }
        // cu zoom activ, fereastra bulei prinde atingerile (doar pe ea); altfel lasă totul să treacă prin ea
        val touchable = settings.bubbleZoom && (kind == Kind.OFFER || kind == Kind.DETAIL || kind == Kind.COMPACT)
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        flags = if (touchable) {
            flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (dm.heightPixels * settings.yPercent.coerceIn(5, 80) / 100f).roundToInt()
        }
    }

    private companion object {
        const val MAX_PANEL_DP = 330f
        const val SMALL_SCREEN_DP = 340f
        /** înălțimea aproximativă a bulei la 100%, ca să nu o lăsăm să coboare peste butoanele Acceptă / Refuză */
        const val OFFER_HEIGHT_DP = 170f
        const val DETAIL_HEIGHT_DP = 320f
        const val COMPACT_HEIGHT_DP = 64f
        /** bula nu trece de acest punct din înălțimea ecranului când e mărită */
        const val SAFE_BOTTOM = 0.55f
    }
}
