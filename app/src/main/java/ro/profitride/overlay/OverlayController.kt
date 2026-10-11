package ro.profitride.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import kotlin.math.min
import kotlin.math.roundToInt
import ro.profitride.core.DriverProfile
import ro.profitride.core.OverlaySettings
import ro.profitride.core.Verdict

/**
 * Pune bula într-o fereastră de tip accesibilitate. Fereastra nu poate lua focus și nu primește atingeri,
 * deci tot ce atinge șoferul ajunge direct în Bolt sau Uber. Poziția pe verticală vine din setări
 * (procent din înălțimea ecranului) și lățimea se adaptează automat la ecran.
 */
class OverlayController(private val service: AccessibilityService) {

    private enum class Kind { OFFER, COMPACT, IDLE, NOTE }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: OverlayView? = null
    private var added = false
    private var lastKind = Kind.IDLE
    private var lastSettings = OverlaySettings()

    val isVisible: Boolean get() = added

    fun showOffer(
        verdict: Verdict,
        vsAverage: Double?,
        profile: DriverProfile,
        settings: OverlaySettings,
        demo: Boolean = false,
    ) {
        val compact = settings.compact || isSmallScreen()
        place(if (compact) Kind.COMPACT else Kind.OFFER, settings)
        view?.showOffer(verdict, vsAverage, profile, compact, demo)
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
    fun relayout(settings: OverlaySettings) {
        lastSettings = settings
        val v = view ?: return
        if (added) runCatching { windowManager.updateViewLayout(v, layoutParams(lastKind, settings)) }
    }

    private fun place(kind: Kind, settings: OverlaySettings) {
        lastKind = kind
        lastSettings = settings
        val v = view ?: OverlayView(service).also { view = it }
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
        val screenW = dm.widthPixels
        val margin = (16 * dm.density).roundToInt()
        val width = when (kind) {
            Kind.OFFER -> min(screenW - 2 * margin, (MAX_PANEL_DP * dm.density).roundToInt())
            else -> WindowManager.LayoutParams.WRAP_CONTENT
        }
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (dm.heightPixels * settings.yPercent.coerceIn(5, 80) / 100f).roundToInt()
        }
    }

    private companion object {
        const val MAX_PANEL_DP = 330f
        const val SMALL_SCREEN_DP = 340f
    }
}
