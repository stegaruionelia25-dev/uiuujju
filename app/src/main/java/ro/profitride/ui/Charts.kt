package ro.profitride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Inel de progres pentru obiectivul zilnic. */
@Composable
fun ProgressRing(progress: Float, label: String, modifier: Modifier = Modifier) {
    val track = PrColors.Surface2
    val color = PrColors.Accent
    Box(modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(84.dp)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
    }
}

/** Grafic cu bare: valoarea fiecărui interval. Valorile negative apar ca o bară mică roșie. */
@Composable
fun BarChart(values: List<Double>, modifier: Modifier = Modifier) {
    val good = PrColors.Accent
    val bad = PrColors.Red
    val track = PrColors.Surface2
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        if (values.isEmpty()) return@Canvas
        val max = values.maxOf { it }.coerceAtLeast(0.01)
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
        val radius = CornerRadius(minOf(barWidth / 2, 4.dp.toPx()))
        values.forEachIndexed { i, v ->
            val x = i * (barWidth + gap)
            drawRoundRect(track, Offset(x, size.height - 3.dp.toPx()), Size(barWidth, 3.dp.toPx()), radius)
            if (v > 0) {
                val h = (size.height * (v / max).toFloat()).coerceAtLeast(3.dp.toPx())
                drawRoundRect(good, Offset(x, size.height - h), Size(barWidth, h), radius)
            } else if (v < 0) {
                drawRoundRect(bad, Offset(x, size.height - 8.dp.toPx()), Size(barWidth, 8.dp.toPx()), radius)
            }
        }
    }
}
