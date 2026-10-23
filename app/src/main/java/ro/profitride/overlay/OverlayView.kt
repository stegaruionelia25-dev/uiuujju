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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import java.util.Locale
import kotlin.math.roundToInt
import ro.profitride.R
import ro.profitride.core.Countries
import ro.profitride.core.DriverProfile
import ro.profitride.core.AppTheme
import ro.profitride.core.BubbleScale
import ro.profitride.core.IdleStyle
import ro.profitride.core.Metric
import ro.profitride.core.OfferFigures
import ro.profitride.core.VerdictBasis
import ro.profitride.core.Rating
import ro.profitride.core.ThemeSpec
import ro.profitride.core.Themes
import ro.profitride.core.Verdict
import ro.profitride.util.Format

/**
 * Bula afișată peste Bolt/Uber. Este un View clasic (nu Compose) ca să fie ușor și stabil
 * într-o fereastră de overlay. Implicit nu primește atingeri (fereastra e NOT_TOUCHABLE). Doar dacă șoferul
 * a activat zoomul, fereastra verdictului prinde atingerile pe suprafața bulei, ca să poată fi mărită cu două degete.
 */
@SuppressLint("ViewConstructor")
class OverlayView(context: Context) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun px(v: Int): Int = (v * density).roundToInt()
    private fun sp(view: TextView, size: Float) = view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)

    private fun font(res: Int, fallback: Typeface): Typeface =
        runCatching { ResourcesCompat.getFont(context, res) }.getOrNull() ?: fallback

    private val medium = font(R.font.nunito_semibold, Typeface.create("sans-serif-medium", Typeface.NORMAL))
    private val bold = font(R.font.nunito_extrabold, Typeface.create("sans-serif-medium", Typeface.BOLD))
    private val black = font(R.font.nunito_black, Typeface.create("sans-serif-medium", Typeface.BOLD))

    private var theme: ThemeSpec = Themes.spec(AppTheme.BLUE)
    private fun c(v: Long): Int = v.toInt()

    // --- mărime și zoom cu două degete ---
    /** factorul aplicat acum bulei (după limitele de ecran) */
    private var drawScale = 1f
    var zoomEnabled = false
    /** procentul dorit de șofer; pornește de la setarea salvată și se modifică în timpul gestului */
    private var wantedPct = BubbleScale.DEFAULT.toFloat()
    var onPinch: ((Int) -> Unit)? = null
    var onPinchEnd: ((Int) -> Unit)? = null
    private val pinch = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = zoomEnabled

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                wantedPct = (wantedPct * detector.scaleFactor).coerceIn(BubbleScale.MIN.toFloat(), BubbleScale.MAX.toFloat())
                onPinch?.invoke(wantedPct.roundToInt())
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                onPinchEnd?.invoke(wantedPct.roundToInt())
            }
        },
    )

    fun setScaleState(effective: Float, wantedPercent: Int) {
        wantedPct = wantedPercent.toFloat()
        if (drawScale != effective) {
            drawScale = effective
            requestLayout()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!zoomEnabled) return false
        pinch.onTouchEvent(event)
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = drawScale
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val childW = if (mode == MeasureSpec.UNSPECIFIED) widthMeasureSpec else MeasureSpec.makeMeasureSpec((size / s).toInt(), mode)
        var w = 0
        var h = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            child.pivotX = 0f
            child.pivotY = 0f
            child.scaleX = s
            child.scaleY = s
            measureChild(child, childW, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            w = maxOf(w, (child.measuredWidth * s).roundToInt())
            h = maxOf(h, (child.measuredHeight * s).roundToInt())
        }
        setMeasuredDimension(
            if (mode == MeasureSpec.EXACTLY) size else w,
            h,
        )
    }

    /** Schimbă culorile bulei după tema aplicației. */
    fun applyTheme(spec: ThemeSpec) {
        if (spec === theme) return
        theme = spec
        offerPanel.background = bubble(24f)
        detailPanel.background = bubble(26f)
        compactPanel.background = bubble(999f)
        idlePanel.background = bubble(999f)
        noteText.background = bubble(999f)
        netText.setTextColor(c(spec.bubbleText))
        subText.setTextColor(c(spec.bubbleMuted))
        compactNet.setTextColor(c(spec.bubbleText))
        idleTitle.setTextColor(c(spec.bubbleText))
        idleSub.setTextColor(c(spec.bubbleMuted))
        noteText.setTextColor(c(spec.bubbleText))
        divider.setBackgroundColor(c(spec.bubbleTrack))
        ring.trackColor = c(spec.bubbleTrack)
        compactRing.trackColor = c(spec.bubbleTrack)
        detailRing.trackColor = c(spec.bubbleTrack)
        ring.labelColorOverride = if (spec.ringWhite) c(spec.bubbleText) else null
        compactRing.labelColorOverride = ring.labelColorOverride
        detailRing.labelColorOverride = ring.labelColorOverride
    }

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

    // --- panou detaliat ---
    private val detailPanel = LinearLayout(context)
    private val detailRing = RingView(context)
    private val detailBig = TextView(context)
    private val detailChip = TextView(context)
    private val detailPrice = TextView(context)
    private val detailTrip = TextView(context)
    private val detailRowA = LinearLayout(context)
    private val detailRowB = LinearLayout(context)
    private val detailFoot = TextView(context)

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
        buildDetailPanel()
        buildCompactPanel()
        buildIdlePanel()
        buildNote()
        addView(offerPanel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(detailPanel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(compactPanel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(idlePanel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(noteText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        showOnly(null)
    }

    private fun bubble(radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(c(theme.bubbleBg))
        cornerRadius = dp(radius)
        setStroke(px(1), c(theme.bubbleBorder))
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
            typeface = black
            setTextColor(c(theme.bubbleText))
            sp(this, 28f)
            includeFontPadding = false
            maxLines = 1
        }
        subText.apply {
            typeface = medium
            setTextColor(c(theme.bubbleMuted))
            sp(this, 12f)
            maxLines = 1
        }
        col.addView(netText)
        col.addView(subText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(4) })
        header.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        chip.apply {
            typeface = black
            sp(this, 11.5f)
            letterSpacing = 0.04f
            maxLines = 1
            setPadding(px(10), px(6), px(10), px(6))
        }
        header.addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = px(8) })
        offerPanel.addView(header)

        divider.setBackgroundColor(c(theme.bubbleTrack))
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

    private fun buildDetailPanel() {
        detailPanel.orientation = LinearLayout.VERTICAL
        detailPanel.background = bubble(26f)
        detailPanel.setPadding(px(16), px(14), px(16), px(14))

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(detailRing, LinearLayout.LayoutParams(px(54), px(54)).apply { marginEnd = px(12) })
        detailBig.apply {
            typeface = black
            setTextColor(c(theme.bubbleText))
            sp(this, 34f)
            includeFontPadding = false
            maxLines = 1
        }
        header.addView(detailBig, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        detailChip.apply {
            typeface = black
            sp(this, 11.5f)
            letterSpacing = 0.04f
            maxLines = 1
            setPadding(px(10), px(6), px(10), px(6))
        }
        header.addView(detailChip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = px(8) })
        detailPanel.addView(header)

        detailPrice.apply {
            typeface = black
            sp(this, 18f)
            maxLines = 1
        }
        detailPanel.addView(detailPrice, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(10) })
        detailTrip.apply {
            typeface = medium
            sp(this, 11.5f)
            maxLines = 2
        }
        detailPanel.addView(detailTrip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(3) })

        detailRowA.orientation = LinearLayout.HORIZONTAL
        detailRowB.orientation = LinearLayout.HORIZONTAL
        detailPanel.addView(detailRowA, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(10) })
        detailPanel.addView(detailRowB, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(8) })

        detailFoot.apply {
            typeface = bold
            sp(this, 12f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        detailPanel.addView(detailFoot, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(10) })
    }

    /** O casetă cu bordură: etichetă mică, valoare mare și, opțional, o linie colorată dedesubt. */
    private fun tile(label: String, value: String, sub: String? = null, subColor: Int = 0): LinearLayout {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(px(6), px(9), px(6), px(9))
            background = GradientDrawable().apply {
                setColor(0x00000000)
                cornerRadius = dp(16f)
                setStroke(px(1), c(theme.bubbleBorder))
            }
        }
        box.addView(TextView(context).apply {
            text = label
            typeface = bold
            setTextColor(c(theme.bubbleMuted))
            sp(this, 10.5f)
            maxLines = 1
            gravity = Gravity.CENTER_HORIZONTAL
        })
        box.addView(TextView(context).apply {
            text = value
            typeface = black
            setTextColor(c(theme.bubbleText))
            sp(this, 18f)
            maxLines = 1
            gravity = Gravity.CENTER_HORIZONTAL
        })
        if (sub != null) {
            box.addView(TextView(context).apply {
                text = sub
                typeface = black
                setTextColor(subColor)
                sp(this, 12f)
                maxLines = 1
                gravity = Gravity.CENTER_HORIZONTAL
            })
        }
        return box
    }

    private fun addTiles(row: LinearLayout, tiles: List<LinearLayout>) {
        row.removeAllViews()
        tiles.forEachIndexed { i, t ->
            row.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) marginStart = px(8)
            })
        }
    }

    /** Valoarea și unitatea unei cifre, în limba și moneda șoferului. */
    private fun figure(metric: Metric, v: Verdict, profile: DriverProfile): Pair<String, String> {
        val country = Countries.byCode(profile.countryCode)
        val cur = OverlayText.currency(profile)
        val x = OfferFigures.value(metric, v, country.unit.kmPerUnit)
        return when (metric) {
            Metric.PER_KM -> Format.number(x, 2) to "$cur/${country.unit.symbol}"
            Metric.PER_HOUR -> Format.number(x, 0) to "$cur/h"
            Metric.NET -> Format.signed(x, 1) to context.getString(R.string.net_suffix, cur)
            Metric.MARGIN -> (Format.number(x, 0) + "%") to context.getString(R.string.ov_margin)
        }
    }

    /** Cifra mare: valoarea mare, unitatea mai mică și mai stearsă. Urmează baza verdictului. */
    private fun bigFigure(v: Verdict, profile: DriverProfile, unitSize: Float): SpannableStringBuilder {
        val (value, _) = figure(OfferFigures.bigMetric(profile.basis), v, profile)
        val unit = OverlayText.basisUnit(context, profile.basis, profile)
        return SpannableStringBuilder(value).apply {
            append(" $unit")
            setSpan(RelativeSizeSpan(unitSize), value.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(c(theme.bubbleMuted)), value.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun bindChip(view: TextView, verdict: Verdict, demo: Boolean) {
        val pillText = c(OverlayText.pillColor(verdict.rating, theme))
        if (demo) {
            view.text = context.getString(R.string.ov_demo)
            view.setTextColor(c(theme.bubbleMuted))
            view.background = pill(c(theme.bubbleTrack))
        } else {
            view.text = OverlayText.chipText(context, verdict.rating)
            view.setTextColor(pillText)
            view.background = pill(if (theme.chipSolid) 0xFFFFFFFF.toInt() else OverlayColors.withAlpha(pillText, 0.16f))
        }
    }

    /** Marginea cardului ia culoarea verdictului (albă în tema Blue). */
    private fun edged(radius: Float, rating: Rating): GradientDrawable {
        val pillText = c(OverlayText.pillColor(rating, theme))
        return GradientDrawable().apply {
            setColor(c(theme.bubbleBg))
            cornerRadius = dp(radius)
            setStroke(px(2), if (theme.ringWhite) c(theme.bubbleText) else pillText)
        }
    }

    private fun buildCompactPanel() {
        compactPanel.orientation = LinearLayout.HORIZONTAL
        compactPanel.gravity = Gravity.CENTER_VERTICAL
        compactPanel.background = bubble(999f)
        compactPanel.setPadding(px(8), px(7), px(16), px(7))
        compactPanel.addView(compactRing, LinearLayout.LayoutParams(px(32), px(32)).apply { marginEnd = px(10) })
        compactNet.apply {
            typeface = black
            setTextColor(c(theme.bubbleText))
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
            typeface = black
            setTextColor(c(theme.bubbleText))
            sp(this, 19f)
            maxLines = 1
        }
        idleSub.apply {
            typeface = medium
            setTextColor(c(theme.bubbleMuted))
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
            typeface = bold
            setTextColor(c(theme.bubbleText))
            sp(this, 13f)
            background = bubble(999f)
            setPadding(px(16), px(10), px(16), px(10))
            maxWidth = px(300)
        }
    }

    private fun showOnly(which: View?) {
        listOf(offerPanel, detailPanel, compactPanel, idlePanel, noteText).forEach {
            it.visibility = if (it === which) VISIBLE else GONE
        }
        if (which !== idlePanel) {
            stopRotation()
            radar.stop()
        }
    }

    // ------------------------------------------------------------------ verdict

    fun showOffer(verdict: Verdict, vsAverage: Double?, profile: DriverProfile, compact: Boolean, demo: Boolean, detailed: Boolean = false) {
        val country = Countries.byCode(profile.countryCode)
        val cur = OverlayText.currency(profile)
        val pillText = c(OverlayText.pillColor(verdict.rating, theme))
        // inelul: alb în tema Blue, altfel în culoarea verdictului
        val color = if (theme.ringWhite) c(theme.bubbleText) else pillText
        val netLabel = Format.signed(verdict.net, 1)
        // culoarea cifrelor „de accent”: galbenă în Blue (unde pastila e albă), altfel culoarea verdictului
        val accent = if (theme.chipSolid) c(theme.hi) else pillText

        if (compact) {
            compactRing.set(verdict.score, color)
            compactNet.text = SpannableStringBuilder(netLabel).apply {
                append(" $cur")
                setSpan(RelativeSizeSpan(0.65f), netLabel.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(c(theme.bubbleMuted)), netLabel.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            showOnly(compactPanel)
        } else if (detailed) {
            showDetail(verdict, profile, demo, color, accent)
        } else {
            ring.set(verdict.score, color)
            netText.text = bigFigure(verdict, profile, 0.4f)
            val minutes = verdict.offer.totalMin.roundToInt()
            subText.text = if (profile.basis == VerdictBasis.NET) {
                context.getString(
                    R.string.ov_net_sub,
                    Format.distance(verdict.offer.totalKm, country.unit),
                    country.unit.symbol,
                    minutes,
                )
            } else {
                context.getString(R.string.ov_sub_net, "$netLabel $cur", minutes)
            }
            bindChip(chip, verdict, demo)
            offerPanel.background = edged(24f, verdict.rating)

            val stats = OfferFigures.statMetrics(profile.basis)
            listOf(statLeft, statMid, statRight).forEachIndexed { i, tv ->
                val (value, unit) = figure(stats[i], verdict, profile)
                bindStat(tv, value, unit)
            }

            if (vsAverage != null) {
                val up = vsAverage >= 0
                val arrow = if (up) "▲ " else "▼ "
                val value = Format.signed(vsAverage, 0)
                val tail = " $cur/h "
                val suffix = context.getString(R.string.ov_vs_avg)
                deltaText.text = SpannableStringBuilder().apply {
                    append(arrow + value + tail)
                    setSpan(ForegroundColorSpan(c(if (up) theme.deltaUp else theme.deltaDown)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val start = length
                    append(suffix)
                    setSpan(ForegroundColorSpan(c(theme.bubbleMuted)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        detailPanel.contentDescription = offerPanel.contentDescription
    }

    /** Cardul Detaliat: cifra mare după baza verdictului, cursa, ridicarea, celelalte cifre, costurile și pragul. */
    private fun showDetail(verdict: Verdict, profile: DriverProfile, demo: Boolean, ringColor: Int, accent: Int) {
        val country = Countries.byCode(profile.countryCode)
        val cur = OverlayText.currency(profile)
        val offer = verdict.offer
        val unit = country.unit

        detailRing.set(verdict.score, ringColor)
        detailBig.text = bigFigure(verdict, profile, 0.36f)
        bindChip(detailChip, verdict, demo)
        detailPanel.background = edged(26f, verdict.rating)

        detailPrice.setTextColor(c(theme.bubbleText))
        detailPrice.text = context.getString(R.string.ov_detail_price, Format.number(offer.price, 2) + " " + cur)
        detailTrip.setTextColor(c(theme.bubbleMuted))
        detailTrip.text = context.getString(
            R.string.ov_detail_trip,
            offer.pickupMin.roundToInt(),
            Format.distance(offer.pickupKm, unit),
            offer.tripMin.roundToInt(),
            Format.distance(offer.tripKm, unit),
            unit.symbol,
        )

        // rândul A: total + celelalte două cifre (fără marjă), niciuna nu repetă cifra mare
        val others = OfferFigures.statMetrics(profile.basis).filter { it != Metric.MARGIN }
        val rowA = mutableListOf(
            tile(
                context.getString(R.string.ov_detail_total),
                Format.distance(offer.totalKm, unit) + " " + unit.symbol,
                offer.totalMin.roundToInt().toString() + " min",
                accent,
            ),
        )
        others.forEach { m ->
            val (value, u) = figure(m, verdict, profile)
            rowA += tile(u, value)
        }
        addTiles(detailRowA, rowA)

        // rândul B: unde se duc banii
        val rowB = mutableListOf(
            tile(context.getString(R.string.ov_detail_commission), Format.number(verdict.commission + verdict.fleetFee, 2) + " " + cur),
            tile(context.getString(R.string.ov_detail_car_cost), Format.number(verdict.carCost, 2) + " " + cur),
        )
        if (verdict.taxReserve > 0) {
            rowB += tile(context.getString(R.string.ov_detail_tax), Format.number(verdict.taxReserve, 2) + " " + cur)
        }
        addTiles(detailRowB, rowB)

        val digits = if (profile.basis == VerdictBasis.PER_KM) 2 else 0
        val threshold = Format.number(profile.goodThreshold, digits) + " " + OverlayText.basisUnit(context, profile.basis, profile)
        detailFoot.setTextColor(accent)
        detailFoot.text = context.getString(
            when (verdict.rating) {
                Rating.GOOD -> R.string.ov_detail_foot_good
                Rating.MEDIUM -> R.string.ov_detail_foot_mid
                Rating.POOR -> R.string.ov_detail_foot_bad
            },
            threshold,
        )
        showOnly(detailPanel)
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(999f)
    }

    private fun bindStat(view: TextView, value: String, unit: String) {
        view.text = SpannableStringBuilder().apply {
            append(value)
            setSpan(ForegroundColorSpan(c(theme.bubbleText)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val start = length
            append(" $unit")
            setSpan(RelativeSizeSpan(0.73f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(c(theme.bubbleMuted)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        view.typeface = bold
        sp(view, 15f)
    }

    // ------------------------------------------------------------------ așteptare

    fun showIdle(messages: List<String>, pauseMinutes: Int?, style: IdleStyle) {
        val paused = pauseMinutes != null
        val accent = c(if (paused) theme.amber else theme.good)
        radar.color = accent
        radar.pulseStyle = style == IdleStyle.PULSE

        if (paused) {
            idleTitle.text = context.getString(R.string.ov_pause, pauseMinutes)
            idleTitle.setTextColor(c(theme.amber))
        } else {
            idleTitle.text = context.getString(R.string.ov_searching) + "…"
            idleTitle.setTextColor(c(theme.bubbleText))
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
