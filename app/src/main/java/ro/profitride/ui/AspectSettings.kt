package ro.profitride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ro.profitride.R
import ro.profitride.core.AppTheme
import ro.profitride.core.BubbleScale
import ro.profitride.core.OverlaySettings
import ro.profitride.core.Themes

internal fun themeName(t: AppTheme) = when (t) {
    AppTheme.BLUE -> R.string.theme_blue
    AppTheme.NOAPTE -> R.string.theme_noapte
    AppTheme.DESCHIS -> R.string.theme_deschis
    AppTheme.EMBER -> R.string.theme_ember
}

internal fun themeTag(t: AppTheme) = when (t) {
    AppTheme.BLUE -> R.string.theme_blue_tag
    AppTheme.NOAPTE -> R.string.theme_noapte_tag
    AppTheme.DESCHIS -> R.string.theme_deschis_tag
    AppTheme.EMBER -> R.string.theme_ember_tag
}

/** Alegerea temei: Blue (implicită), Noapte, Deschis, Ember, plus „Automat, după lumină”. */
@Composable
fun AspectCard(overlay: OverlaySettings, update: ((OverlaySettings) -> OverlaySettings) -> Unit) {
    PrCard {
        Label(stringResource(R.string.style_title))
        StyleChooser(overlay.offerStyle) { st -> update { it.copy(offerStyle = st) } }
        Hint(stringResource(R.string.style_hint))
        Label(stringResource(R.string.aspect_choose))
        AppTheme.entries.forEach { t ->
            val spec = Themes.spec(t)
            val selected = overlay.appTheme == t
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { update { it.copy(appTheme = t) } },
                shape = RoundedCornerShape(22.dp),
                color = PrColors.Surface2,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrColors.Hi else PrColors.Outline),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(spec.bg)),
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(spec.surface, spec.brand, spec.good).forEach {
                            Box(Modifier.size(14.dp).background(Color(it), CircleShape))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(themeName(t)), fontSize = 16.sp, fontWeight = FontWeight.Black, color = PrColors.Text)
                        Text(stringResource(themeTag(t)), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrColors.Muted)
                    }
                    Box(
                        Modifier
                            .size(26.dp)
                            .then(
                                if (selected) Modifier.background(PrColors.Brand, CircleShape)
                                else Modifier.border(3.dp, PrColors.Faint, CircleShape),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Text("✓", color = PrColors.OnBrand, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
        SwitchLine(
            title = stringResource(R.string.theme_auto_light),
            subtitle = stringResource(R.string.theme_auto_light_sub),
            checked = overlay.autoTheme,
            onChange = { on -> update { it.copy(autoTheme = on) } },
        )
        Hint(stringResource(R.string.theme_hint2))
    }
}

/** Mărimea bulei de verdict: slider, trei presetări și comutatorul pentru zoom cu două degete. */
@Composable
fun BubbleSizeCard(
    overlay: OverlaySettings,
    update: ((OverlaySettings) -> OverlaySettings) -> Unit,
    onPreview: () -> Unit,
) {
    val pct = BubbleScale.clamp(overlay.bubbleScale)
    PrCard {
        Label(stringResource(R.string.bubble_title))
        BubblePreview(pct)
        Text(stringResource(R.string.bubble_size, pct), color = PrColors.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Slider(
            value = pct.toFloat(),
            onValueChange = { v -> update { it.copy(bubbleScale = BubbleScale.clamp(v.roundToInt())) } },
            onValueChangeFinished = onPreview,
            valueRange = BubbleScale.MIN.toFloat()..BubbleScale.MAX.toFloat(),
            colors = SliderDefaults.colors(thumbColor = PrColors.Brand, activeTrackColor = PrColors.Brand),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.bubble_small to 80, R.string.bubble_normal to 100, R.string.bubble_big to 130).forEach { (label, value) ->
                val on = pct == value
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) PrColors.Brand else PrColors.Surface2)
                        .clickable {
                            update { it.copy(bubbleScale = value) }
                            onPreview()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(label),
                        color = if (on) PrColors.OnBrand else PrColors.Muted,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        SwitchLine(
            title = stringResource(R.string.bubble_zoom),
            subtitle = stringResource(R.string.bubble_zoom_sub),
            checked = overlay.bubbleZoom,
            onChange = { on -> update { it.copy(bubbleZoom = on) } },
        )
        Hint(stringResource(R.string.bubble_hint))
    }
}

/** Previzualizare mică: o bulă simplificată, la mărimea aleasă, peste o hartă stilizată. */
@Composable
private fun BubblePreview(pct: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1B2027)),
        contentAlignment = Alignment.TopCenter,
    ) {
        val s = 0.62f * pct / 100f
        Surface(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(280.dp)
                .scale(s),
            shape = RoundedCornerShape(26.dp),
            color = PrColors.Bg,
            border = BorderStroke(1.dp, PrColors.Outline),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(46.dp).background(PrColors.Surface2, CircleShape), contentAlignment = Alignment.Center) {
                    Text("84", color = PrColors.Text, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("+16,9 lei", color = PrColors.Text, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("11,5 km · 24 min", color = PrColors.Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Box(
                    Modifier
                        .background(PrColors.Surface2, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("✓", color = PrColors.Accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SwitchLine(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = PrColors.Text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = PrColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrColors.Brand, checkedThumbColor = PrColors.OnBrand),
        )
    }
}
