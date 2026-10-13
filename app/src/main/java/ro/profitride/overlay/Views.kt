package ro.profitride.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

object OverlayColors {
    const val GREEN = 0xFF2EE6A6.toInt()
    const val AMBER = 0xFFFFB020.toInt()
    const val RED = 0xFFFF5A66.toInt()
    const val RED_SOFT_TEXT = 0xFFFF8A93.toInt()
    const val TEXT = 0xFFEEF2F6.toInt()
    const val MUTED = 0xFF9AA6B2.toInt()
    const val PANEL = 0xF00E1116.toInt()
    const val PANEL_INNER = 0xFF0E1116.toInt()

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (a shl 24) or (color and 0x00FFFFFF)
    }
}

/** Inel de progres cu scorul 0–100 în centru. */
class RingView(context: Context) : View(context) {

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x24FFFFFF
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val oval = RectF()

    private var score = 0
    private var ringColor = OverlayColors.GREEN

    fun set(score: Int, color: Int) {
        this.score = score.coerceIn(0, 100)
        ringColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val stroke = size * 0.095f
        track.strokeWidth = stroke
        arc.strokeWidth = stroke
        arc.color = ringColor
        val inset = stroke / 2f
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        oval.set(left + inset, top + inset, left + size - inset, top + size - inset)
        canvas.drawArc(oval, 0f, 360f, false, track)
        canvas.drawArc(oval, -90f, 360f * score / 100f, false, arc)

        label.color = ringColor
        label.textSize = size * 0.32f
        val cy = height / 2f - (label.ascent() + label.descent()) / 2f
        canvas.drawText(score.toString(), width / 2f, cy, label)
    }
}

/**
 * Animația din așteptare: radar (cercuri care se extind + fascicul rotitor) sau puls (linie de bătăi).
 * Se oprește singură când nu e vizibilă și când animațiile sunt dezactivate din sistem.
 */
class RadarView(context: Context) : View(context) {

    var pulseStyle: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var color: Int = OverlayColors.GREEN
        set(value) {
            field = value
            sweepShader = null
            invalidate()
        }

    private var sweepShader: SweepGradient? = null
    private var phase = 0.3f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val ecg = Path()
    private val ecgSegment = Path()
    private val measure = PathMeasure()
    private var ecgLength = 0f

    fun start() {
        if (!isShown) return
        if (ValueAnimator.areAnimatorsEnabled()) {
            if (!animator.isStarted) animator.start()
        } else {
            phase = 0.3f
            invalidate()
        }
    }

    fun stop() {
        animator.cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sweepShader = null
        val s = min(w, h).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val x0 = cx - s * 0.36f
        val unit = s * 0.72f / 120f
        fun px(x: Float) = x0 + x * unit
        fun py(y: Float) = cy + (y - 20f) * unit
        ecg.reset()
        ecg.moveTo(px(0f), py(20f))
        ecg.lineTo(px(30f), py(20f))
        ecg.lineTo(px(38f), py(20f))
        ecg.lineTo(px(44f), py(6f))
        ecg.lineTo(px(52f), py(34f))
        ecg.lineTo(px(58f), py(20f))
        ecg.lineTo(px(120f), py(20f))
        measure.setPath(ecg, false)
        ecgLength = measure.length
    }

    override fun onDraw(canvas: Canvas) {
        val s = min(width, height).toFloat()
        if (s <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val r = s / 2f

        if (pulseStyle) drawPulse(canvas, cx, cy, r, s) else drawRadar(canvas, cx, cy, r, s)
    }

    private fun drawRadar(canvas: Canvas, cx: Float, cy: Float, r: Float, s: Float) {
        ring.strokeWidth = s * 0.035f
        for (k in 0..1) {
            val p = (phase + k * 0.5f) % 1f
            val radius = (r - ring.strokeWidth) * (0.25f + 0.75f * p)
            ring.color = OverlayColors.withAlpha(color, (1f - p) * 0.85f)
            canvas.drawCircle(cx, cy, radius, ring)
        }

        val shader = sweepShader ?: SweepGradient(
            cx, cy,
            intArrayOf(
                OverlayColors.withAlpha(color, 0f),
                OverlayColors.withAlpha(color, 0.45f),
                OverlayColors.withAlpha(color, 0f),
                OverlayColors.withAlpha(color, 0f),
            ),
            floatArrayOf(0f, 0.194f, 0.2f, 1f),
        ).also { sweepShader = it }
        sweep.shader = shader
        canvas.save()
        canvas.rotate(phase * 360f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.79f, sweep)
        canvas.restore()

        fill.color = color
        canvas.drawCircle(cx, cy, s * 0.11f, fill)
    }

    private fun drawPulse(canvas: Canvas, cx: Float, cy: Float, r: Float, s: Float) {
        fill.color = OverlayColors.withAlpha(color, 0.12f)
        canvas.drawCircle(cx, cy, r, fill)

        line.strokeWidth = s * 0.07f
        line.color = OverlayColors.withAlpha(color, 0.25f)
        canvas.drawPath(ecg, line)

        if (ecgLength <= 0f) return
        val head = phase * 1.35f * ecgLength
        val tail = (head - 0.35f * ecgLength).coerceAtLeast(0f)
        if (tail < ecgLength) {
            ecgSegment.reset()
            measure.getSegment(tail, min(head, ecgLength), ecgSegment, true)
            line.color = color
            canvas.drawPath(ecgSegment, line)
        }
    }
}
