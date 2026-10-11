package ro.profitride.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt
import ro.profitride.R
import ro.profitride.core.Countries
import ro.profitride.core.DriverProfile
import ro.profitride.core.IdleStyle
import ro.profitride.core.Verdict
import ro.profitride.util.Format

/**
 * Bula afișată peste Bolt/Uber. Este un View clasic (nu Compose) ca să fie ușor și stabil
 * într-o fereastră de overlay. Nu primește atingeri: fereastra e marcată NOT_TOUCHABLE.
 */
@SuppressLint("ViewConstructor")
class OverlayView(context: Context) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun px(v: Int): Int = (v * density).roundToInt()
    private fun sp(view: TextView, size: Float) = view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)

    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val bold = Typeface.create("sans-serif-medium", Typeface.BOLD)

    // --- panou verdict ---
    private val offerPanel = LinearLayout(context)
    private val ring = RingView(context)
    private val netText = TextView(context)
    private val subText = TextView(context)
    private val chip = TextView(context)
    private val divider = View(context)
    private val statLeft = TextView(context)
    private val statMid = TextView(context)
    private val statRight = TextView(context)
    private val deltaText = TextView(context)

    // --- panou compact (un singur rând) ---
    private val compactPanel = LinearLayout(context)
    private val compactRing = RingView(context)
    private val compactNet = TextView(context)

    // --- panou așteptare ---
    private val idlePanel = LinearLayout(context)
    private val radar = RadarView(context)
    private val idleTitle = TextView(context)
    private val idleSub = TextView(context)

    // --- notă scurtă ---
    private val noteText = TextView(context)

    private val handler = Handler(Looper.getMainLooper())
    private var rotating: List<String> = emptyList()
    private var rotateIndex = 0
    private val rotateRunnable = object : Runnable {
        override fun run() {
            if (rotating.size > 1) {
                rotateIndex = (rotateIndex + 1) % rotating.size
                idleSub.animate().alpha(0f).setDuration(180).withEndAction {
                    idleSub.text = rotating[rotateIndex]
                    idleSub.animate().alpha(1f).setDuration(180).start()
                }.start()
                handler.postDelayed(this, ROTATE_MS)
            }
        }
    }

    init {
        buildOfferPanel()
        buildCompactPanel()
        buildIdlePanel()
        buildNote()
        addView(offerPanel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(compactPanel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(idlePanel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(noteText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        showOnly(null)
    }

    private fun bubble(radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(OverlayColors.PANEL)
        cornerRadius = dp(radius)
        setStroke(px(1), 0x1AFFFFFF)
    }

    private fun buildOfferPanel() {
        offerPanel.orientation = LinearLayout.VERTICAL
        offerPanel.background = bubble(24f)
        offerPanel.setPadding(px(14), px(12), px(14), px(12))

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ring, LinearLayout.LayoutParams(px(52), px(52)).apply { marginEnd = px(12) })

        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        netText.apply {
            typeface = bold
            setTextColor(OverlayColors.TEXT)
            sp(this, 30f)
            includeFontPadding = false
            maxLines = 1
        }
        subText.apply {
            typeface = medium
            setTextColor(OverlayColors.MUTED)
            sp(this, 12f)
            maxLines = 1
        }
        col.addView(netText)
        col.addView(subText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(4) })
        header.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        chip.apply {
            typeface = bold
            sp(this, 12f)
            letterSpacing = 0.08f
            setPadding(px(10), px(6), px(10), px(6))
        }
        header.addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = px(8) })
        offerPanel.addView(header)

        divider.setBackgroundColor(0x14FFFFFF)
        offerPanel.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(1)).apply {
            topMargin = px(10)
            bottomMargin = px(10)
        })

        val stats = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(statLeft to Gravity.START, statMid to Gravity.CENTER_HORIZONTAL, statRight to Gravity.END).forEach { (tv, g) ->
            tv.gravity = g
            tv.maxLines = 1
            stats.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        offerPanel.addView(stats)

        deltaText.apply {
            gravity = Gravity.CENTER_HORIZONTAL
            typeface = bold
            sp(this, 12f)
        }
        offerPanel.addView(deltaText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(8) })
    }

    private fun buildCompactPanel() {
        compactPanel.orientation = LinearLayout.HORIZONTAL
        compactPanel.gravity = Gravity.CENTER_VERTICAL
        compactPanel.background = bubble(999f)
        compactPanel.setPadding(px(8), px(7), px(16), px(7))
        compactPanel.addView(compactRing, LinearLayout.LayoutParams(px(32), px(32)).apply { marginEnd = px(10) })
        compactNet.apply {
            typeface = bold
            setTextColor(OverlayColors.TEXT)
            sp(this, 19f)
            maxLines = 1
        }
        compactPanel.addView(compactNet)
    }

    private fun buildIdlePanel() {
        idlePanel.orientation = LinearLayout.HORIZONTAL
        idlePanel.gravity = Gravity.CENTER_VERTICAL
        idlePanel.background = bubble(999f)
        idlePanel.setPadding(px(10), px(8), px(18), px(8))
        idlePanel.addView(radar, LinearLayout.LayoutParams(px(56), px(56)).apply { marginEnd = px(12) })

        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        idleTitle.apply {
            typeface = bold
            setTextColor(OverlayColors.TEXT)
            sp(this, 19f)
            maxLines = 1
        }
        idleSub.apply {
            typeface = medium
            setTextColor(OverlayColors.MUTED)
            sp(this, 12f)
            maxLines = 1
            minWidth = px(150)
        }
        col.addView(idleTitle)
        col.addView(idleSub, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(3) })
        idlePanel.addView(col)
    }

    private fun buildNote() {
        noteText.apply {
            typeface = medium
            setTextColor(OverlayColors.TEXT)
            sp(this, 13f)
            background = bubble(999f)
            setPadding(px(16), px(10), px(16), px(10))
            maxWidth = px(300)
        }
    }

    private fun showOnly(which: View?) {
        listOf(offerPanel, compactPanel, idlePanel, noteText).forEach {
            it.visibility = if (it === which) VISIBLE else GONE
        }
        if (which !== idlePanel) {
            stopRotation()
            radar.stop()
        }
    }

    // ------------------------------------------------------------------ verdict

    fun showOffer(verdict: Verdict, vsAverage: Double?, profile: DriverProfile, compact: Boolean, demo: Boolean) {
        val country = Countries.byCode(profile.countryCode)
        val cur = OverlayText.currency(profile)
        val color = OverlayText.ratingColor(verdict.rating)
        val netLabel = Format.signed(verdict.net, 1)

        if (compact) {
            compactRing.set(verdict.score, color)
            compactNet.text = SpannableStringBuilder(netLabel).apply {
                append(" $cur")
                setSpan(RelativeSizeSpan(0.65f), netLabel.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(OverlayColors.MUTED), netLabel.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            showOnly(compactPanel)
        } else {
            ring.set(verdict.score, color)
            netText.text = SpannableStringBuilder(netLabel).apply {
                append(" $cur")
                setSpan(RelativeSizeSpan(0.5f), netLabel.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(OverlayColors.MUTED), netLabel.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            subText.text = context.getString(
                R.string.ov_net_sub,
                Format.distance(verdict.offer.totalKm, country.unit),
                country.unit.symbol,
                verdict.offer.totalMin.roundToInt(),
            )
            if (demo) {
                chip.text = context.getString(R.string.ov_demo)
                chip.setTextColor(OverlayColors.MUTED)
                chip.background = pill(0x22FFFFFF)
            } else {
                chip.text = OverlayText.ratingLabel(context, verdict.rating)
                chip.setTextColor(color)
                chip.background = pill(OverlayColors.withAlpha(color, 0.16f))
            }

            val perUnit = verdict.perKm * country.unit.kmPerUnit
            bindStat(statLeft, Format.number(perUnit, 2), "$cur/${country.unit.symbol}")
            bindStat(statMid, Format.number(verdict.perHour, 0), "$cur/h")
            bindStat(statRight, Format.number(verdict.margin * 100, 0) + "%", context.getString(R.string.ov_margin))

            if (vsAverage != null) {
                val up = vsAverage >= 0
                val arrow = if (up) "▲ " else "▼ "
                val value = Format.signed(vsAverage, 0)
                val tail = " $cur/h "
                val suffix = context.getString(R.string.ov_vs_avg)
                deltaText.text = SpannableStringBuilder().apply {
                    append(arrow + value + tail)
                    setSpan(ForegroundColorSpan(if (up) OverlayColors.GREEN else OverlayColors.RED_SOFT_TEXT), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val start = length
                    append(suffix)
                    setSpan(ForegroundColorSpan(OverlayColors.MUTED), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(StyleSpan(Typeface.NORMAL), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                deltaText.visibility = VISIBLE
            } else {
                deltaText.visibility = GONE
            }
            showOnly(offerPanel)
        }

        offerPanel.contentDescription = context.getString(
            R.string.ov_a11y_offer,
            OverlayText.ratingLabel(context, verdict.rating),
            "$netLabel $cur",
            Format.distance(verdict.offer.totalKm, country.unit) + " " + country.unit.symbol,
            verdict.offer.totalMin.roundToInt(),
        )
        compactPanel.contentDescription = offerPanel.contentDescription
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(999f)
    }

    private fun bindStat(view: TextView, value: String, unit: String) {
        view.text = SpannableStringBuilder().apply {
            append(value)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(OverlayColors.TEXT), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val start = length
            append(" $unit")
            setSpan(RelativeSizeSpan(0.73f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(OverlayColors.MUTED), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        view.typeface = bold
        sp(view, 15f)
    }

    // ------------------------------------------------------------------ așteptare

    fun showIdle(messages: List<String>, pauseMinutes: Int?, style: IdleStyle) {
        val paused = pauseMinutes != null
        val accent = if (paused) OverlayColors.AMBER else OverlayColors.GREEN
        radar.color = accent
        radar.pulseStyle = style == IdleStyle.PULSE

        if (paused) {
            idleTitle.text = context.getString(R.string.ov_pause, pauseMinutes)
            idleTitle.setTextColor(OverlayColors.AMBER)
        } else {
            idleTitle.text = context.getString(R.string.ov_searching) + "…"
            idleTitle.setTextColor(OverlayColors.TEXT)
        }

        rotating = messages
        rotateIndex = 0
        idleSub.animate().cancel()
        idleSub.alpha = 1f
        idleSub.text = messages.firstOrNull().orEmpty()
        idleSub.visibility = if (messages.isEmpty()) GONE else VISIBLE

        showOnly(idlePanel)
        radar.start()
        handler.removeCallbacks(rotateRunnable)
        if (messages.size > 1) handler.postDelayed(rotateRunnable, ROTATE_MS)
    }

    fun showNote(text: String) {
        noteText.text = text
        showOnly(noteText)
    }

    private fun stopRotation() {
        handler.removeCallbacks(rotateRunnable)
        idleSub.animate().cancel()
    }

    fun stopAnimations() {
        stopRotation()
        radar.stop()
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val ROTATE_MS = 4000L
    }
}
