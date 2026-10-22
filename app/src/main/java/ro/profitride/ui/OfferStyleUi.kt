package ro.profitride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.profitride.R
import ro.profitride.core.AppTheme
import ro.profitride.core.OfferStyle
import ro.profitride.core.OverlaySettings
import ro.profitride.core.Rating
import ro.profitride.core.ThemeSpec
import ro.profitride.core.Themes
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

// Exemplu fix, doar pentru previzualizări: nu sunt date reale. Baza verdictului în exemplu: lei/km.
private const val SAMPLE_SCORE = 84

private fun styleName(style: OfferStyle) = when (style) {
    OfferStyle.COMPACT -> R.string.style_compact
    OfferStyle.DETAILED -> R.string.style_detailed
}

private fun styleSub(style: OfferStyle) = when (style) {
    OfferStyle.COMPACT -> R.string.style_compact_sub
    OfferStyle.DETAILED -> R.string.style_detailed_sub
}

/** Alegerea stilului și a temei, cu previzualizare live: pasul „Alege cum arată” din instalare. */
@Composable
fun LookPicker(overlay: OverlaySettings, update: ((OverlaySettings) -> OverlaySettings) -> Unit, cur: String, unit: String) {
    val theme = effectiveTheme(overlay.appTheme, overlay.autoTheme)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OfferPreview(overlay.offerStyle, theme, cur, unit)
        Label(stringResource(R.string.style_title))
        StyleChooser(overlay.offerStyle) { s -> update { it.copy(offerStyle = s) } }
        Label(stringResource(R.string.ob_look_theme))
        ThemeSwatches(overlay.appTheme) { t -> update { it.copy(appTheme = t) } }
    }
}

/** Două carduri: Compact și Detaliat. */
@Composable
fun StyleChooser(selected: OfferStyle, onSelect: (OfferStyle) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OfferStyle.entries.forEach { s ->
            val on = s == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(s) },
                shape = RoundedCornerShape(22.dp),
                color = if (on) PrColors.Surface2 else PrColors.Surface,
                border = BorderStroke(if (on) 2.dp else 1.dp, if (on) PrColors.Brand else PrColors.Outline),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleGlyph(s)
                    Column {
                        Text(stringResource(styleName(s)), color = PrColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(stringResource(styleSub(s)), color = PrColors.Muted, fontSize = 11.5.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

/** Mică schiță a stilului: o pastilă (Compact) sau o grilă (Detaliat). */
@Composable
private fun StyleGlyph(style: OfferStyle) {
    val c = PrColors.Brand
    Box(Modifier.height(34.dp), contentAlignment = Alignment.CenterStart) {
        if (style == OfferStyle.COMPACT) {
            Box(Modifier.size(width = 44.dp, height = 22.dp).background(c, RoundedCornerShape(8.dp)))
        } else {
            Column(Modifier.size(width = 44.dp, height = 32.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.fillMaxWidth().height(9.dp).background(c, RoundedCornerShape(4.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { Box(Modifier.weight(1f).height(8.dp).background(c.copy(alpha = 0.7f), RoundedCornerShape(3.dp))) }
                }
                Box(Modifier.fillMaxWidth().height(7.dp).background(c.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
            }
        }
    }
}

/** Patru cercuri colorate, câte unul pentru fiecare temă. */
@Composable
fun ThemeSwatches(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppTheme.entries.forEach { t ->
            val spec = Themes.spec(t)
            val on = t == selected
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .then(if (on) Modifier.border(3.dp, PrColors.Brand, CircleShape) else Modifier.border(1.dp, PrColors.Outline, CircleShape))
                        .padding(if (on) 4.dp else 0.dp)
                        .clip(CircleShape)
                        .background(Color(spec.bg)),
                ) {
                    Box(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 8.dp).size(14.dp).background(Color(spec.brand), CircleShape))
                    Box(Modifier.align(Alignment.TopEnd).padding(end = 9.dp, top = 9.dp).size(10.dp).background(Color(spec.pillGood), CircleShape))
                }
                Text(
                    stringResource(themeName(t)),
                    color = if (on) PrColors.Text else PrColors.Muted,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.Black else FontWeight.Bold,
                )
            }
        }
    }
}

/** Previzualizare: cardul ales, în tema aleasă, peste o hartă stilizată. */
@Composable
fun OfferPreview(style: OfferStyle, theme: AppTheme, cur: String, unit: String, modifier: Modifier = Modifier) {
    val spec = Themes.spec(theme)
    Box(
        modifier
            .fillMaxWidth()
            .height(286.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1B2027)),
    ) {
        Box(Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 14.dp)) {
            if (style == OfferStyle.COMPACT) SampleCompactCard(spec, cur, unit) else SampleDetailCard(spec, cur, unit)
        }
        Text(
            stringResource(R.string.style_example),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .background(Color(0x8C000000), RoundedCornerShape(99.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun cardShell(spec: ThemeSpec, content: @Composable () -> Unit) {
    val edge = if (spec.ringWhite) Color(spec.bubbleText) else Color(spec.pillGood)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(spec.bubbleBg),
        border = BorderStroke(2.5.dp, edge),
        contentColor = Color(spec.bubbleText),
    ) { content() }
}

@Composable
private fun ScoreRing(spec: ThemeSpec, size: Dp) {
    val ring = if (spec.ringWhite) Color(spec.bubbleText) else Color(spec.pillGood)
    val track = Color(spec.bubbleTrack)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = this.size.width * 0.11f
            val inner = Size(this.size.width - sw, this.size.height - sw)
            val corner = Offset(sw / 2f, sw / 2f)
            drawArc(track, -90f, 360f, false, topLeft = corner, size = inner, style = Stroke(sw))
            drawArc(ring, -90f, 360f * SAMPLE_SCORE / 100f, false, topLeft = corner, size = inner, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Text(SAMPLE_SCORE.toString(), color = Color(spec.bubbleText), fontSize = (size.value * 0.3f).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun VerdictChip(spec: ThemeSpec) {
    val good = Color(spec.pillGood)
    val bg = if (spec.chipSolid) Color.White else good.copy(alpha = 0.16f)
    Row(
        Modifier
            .background(bg, RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("✓", color = good, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(OverlayText.ratingLabel(LocalContext.current, Rating.GOOD), color = good, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BigFigure(spec: ThemeSpec, cur: String, unit: String, size: Float) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(Format.number(1.47, 2), color = Color(spec.bubbleText), fontSize = size.sp, fontWeight = FontWeight.Black)
        Text(
            " " + stringResource(R.string.net_suffix, "$cur/$unit"),
            color = Color(spec.bubbleMuted),
            fontSize = (size * 0.36f).sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = (size * 0.12f).dp),
        )
    }
}

@Composable
private fun SampleCompactCard(spec: ThemeSpec, cur: String, unit: String) {
    val muted = Color(spec.bubbleMuted)
    cardShell(spec) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScoreRing(spec, 48.dp)
                Column(Modifier.weight(1f)) {
                    BigFigure(spec, cur, unit, 30f)
                    Text(stringResource(R.string.ov_sub_net, Format.signed(16.9, 1) + " " + cur, 24), color = muted, fontSize = 11.5.sp)
                }
                VerdictChip(spec)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatText(spec, "42", "$cur/h")
                StatText(spec, Format.signed(16.9, 1), stringResource(R.string.net_suffix, cur))
                StatText(spec, "54%", stringResource(R.string.ov_margin))
            }
        }
    }
}

@Composable
private fun StatText(spec: ThemeSpec, value: String, unit: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, color = Color(spec.bubbleText), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(unit, color = Color(spec.bubbleMuted), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SampleDetailCard(spec: ThemeSpec, cur: String, unit: String) {
    val muted = Color(spec.bubbleMuted)
    val accent = if (spec.chipSolid) Color(spec.hi) else Color(spec.pillGood)
    cardShell(spec) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreRing(spec, 44.dp)
                Box(Modifier.weight(1f)) { BigFigure(spec, cur, unit, 28f) }
                VerdictChip(spec)
            }
            Text(stringResource(R.string.ov_detail_price, Format.number(31.5, 2) + " " + cur), color = Color(spec.bubbleText), fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.ov_detail_trip, 5, Format.number(2.1, 1), 19, Format.number(9.4, 1), unit), color = muted, fontSize = 10.5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTile(spec, stringResource(R.string.ov_detail_total), Format.number(11.5, 1) + " $unit", Modifier.weight(1f), "24 min", accent)
                MiniTile(spec, "$cur/h", "42", Modifier.weight(1f))
                MiniTile(spec, stringResource(R.string.net_suffix, cur), Format.signed(16.9, 1), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTile(spec, stringResource(R.string.ov_detail_commission), Format.number(6.3, 2) + " $cur", Modifier.weight(1f))
                MiniTile(spec, stringResource(R.string.ov_detail_car_cost), Format.number(8.3, 2) + " $cur", Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.ov_detail_foot_good, Format.number(1.2, 2) + " " + stringResource(R.string.net_suffix, "$cur/$unit")),
                color = accent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MiniTile(spec: ThemeSpec, label: String, value: String, modifier: Modifier, sub: String? = null, subColor: Color = Color.Unspecified) {
    Column(
        modifier
            .border(1.dp, Color(spec.bubbleBorder), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color(spec.bubbleMuted), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(value, color = Color(spec.bubbleText), fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
        if (sub != null) Text(sub, color = subColor, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
    }
}

/** Acasă: o linie mică cu stilul și tema alese și butonul „Schimbă”, care duce în Setări → Aspect. */
@Composable
fun HomeStyleLine(overlay: OverlaySettings, onChange: () -> Unit) {
    PrCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Label(stringResource(R.string.home_style_label))
                Text(
                    stringResource(R.string.home_style_value, stringResource(styleName(overlay.offerStyle)), stringResource(themeName(overlay.appTheme))),
                    color = PrColors.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            PillButton(stringResource(R.string.home_style_change), onChange)
        }
    }
}

/** Mesajul de o singură dată, după prima ofertă: unde se schimbă stilul. */
@Composable
fun StyleHintCard(onOk: () -> Unit) {
    PrCard(border = PrColors.Hi.copy(alpha = 0.5f)) {
        Text(stringResource(R.string.home_style_hint_title), color = PrColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.home_style_hint_body), color = PrColors.Muted, fontSize = 13.5.sp, lineHeight = 19.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PillButton(stringResource(R.string.home_style_hint_ok), onOk)
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(PrColors.Brand)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = PrColors.OnBrand, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

/** Instalare, „Cum funcționează”: o machetă a aplicației de ride, cu overlay-ul pus pe ea. Trei pagini. */
@Composable
fun IntroStep(page: Int, theme: AppTheme, cur: String, unit: String) {
    val spec = Themes.spec(theme)
    val titles = listOf(R.string.ob_intro1_title, R.string.ob_intro2_title, R.string.ob_intro3_title)
    val subs = listOf(R.string.ob_intro1_sub, R.string.ob_intro2_sub, R.string.ob_intro3_sub)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(width = if (i == page) 20.dp else 7.dp, height = 7.dp)
                        .background(if (i == page) PrColors.Brand else PrColors.Outline, RoundedCornerShape(99.dp)),
                )
            }
        }
        Text(stringResource(R.string.app_name), color = PrColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Box(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF1B2027)),
        ) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color(0xFF14181E), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.ob_mock_new_ride), color = Color(0xFF9AA6B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(Format.number(31.5, 2) + " " + cur, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    stringResource(R.string.ob_mock_decline),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.background(Color(0xFF262D36), RoundedCornerShape(99.dp)).padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
            if (page >= 1) {
                Box(Modifier.align(Alignment.TopCenter).padding(start = 22.dp, end = 22.dp, top = 100.dp)) {
                    SampleCompactCard(spec, cur, unit)
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(50.dp)
                    .background(Color(0xFF1FA971), RoundedCornerShape(25.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.ob_mock_accept), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (page == 2) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(6.dp)
                        .height(62.dp)
                        .border(3.dp, Color.White, RoundedCornerShape(31.dp)),
                )
                Text(
                    stringResource(R.string.ob_intro_you_press),
                    color = Color(0xFF0E1A2B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 84.dp)
                        .background(Color.White, RoundedCornerShape(99.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(titles[page]), color = PrColors.Text, fontSize = 30.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
            Text(stringResource(subs[page]), color = PrColors.Muted, fontSize = 15.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
