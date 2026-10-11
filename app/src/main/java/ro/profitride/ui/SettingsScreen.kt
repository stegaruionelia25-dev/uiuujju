package ro.profitride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import ro.profitride.BuildConfig
import ro.profitride.R
import ro.profitride.core.IdleStyle

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenCalibration: () -> Unit) {
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val serviceEnabled by vm.serviceEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.settings_title))

        PrCard(border = if (serviceEnabled) PrColors.Accent.copy(alpha = 0.4f) else PrColors.Amber.copy(alpha = 0.5f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.perm_accessibility), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                if (serviceEnabled) {
                    StatusPill(stringResource(R.string.perm_active), PrColors.Accent)
                } else {
                    StatusPill(stringResource(R.string.perm_inactive), PrColors.Amber)
                }
            }
            SecondaryButton(
                stringResource(R.string.perm_open_accessibility),
                onClick = { AccessibilityStatus.openAccessibilitySettings(context) },
            )
            Hint(stringResource(R.string.perm_restricted_hint))
            SecondaryButton(
                stringResource(R.string.perm_open_app_details),
                onClick = { AccessibilityStatus.openAppDetails(context) },
            )
        }

        PrCard {
            Label(stringResource(R.string.set_overlay_title))
            Text(
                stringResource(R.string.set_position, overlay.yPercent),
                color = PrColors.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = overlay.yPercent.toFloat(),
                onValueChange = { v -> vm.updateOverlay { it.copy(yPercent = v.roundToInt()) } },
                onValueChangeFinished = { vm.previewOverlay() },
                valueRange = 10f..60f,
                colors = SliderDefaults.colors(thumbColor = PrColors.Accent, activeTrackColor = PrColors.Accent),
            )
            Hint(stringResource(R.string.set_position_hint))
            SecondaryButton(stringResource(R.string.set_preview), onClick = { vm.previewOverlay() })
            if (!serviceEnabled) Hint(stringResource(R.string.set_preview_needs_service))

            SwitchRow(stringResource(R.string.set_compact), stringResource(R.string.set_compact_sub), overlay.compact) { on ->
                vm.updateOverlay { it.copy(compact = on) }
            }
            SwitchRow(stringResource(R.string.set_show_idle), stringResource(R.string.set_show_idle_sub), overlay.showIdle) { on ->
                vm.updateOverlay { it.copy(showIdle = on) }
            }
            if (overlay.showIdle) {
                Label(stringResource(R.string.set_idle_style))
                ChipRow(
                    options = IdleStyle.entries,
                    selected = overlay.idleStyle,
                    label = { stringResourceFor(it) },
                    onSelect = { s -> vm.updateOverlay { it.copy(idleStyle = s) } },
                )
                Text(
                    stringResource(R.string.set_pause_after, overlay.pauseAfterMin),
                    color = PrColors.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = overlay.pauseAfterMin.toFloat(),
                    onValueChange = { v -> vm.updateOverlay { it.copy(pauseAfterMin = v.roundToInt()) } },
                    valueRange = 3f..30f,
                    colors = SliderDefaults.colors(thumbColor = PrColors.Accent, activeTrackColor = PrColors.Accent),
                )
            }
            SwitchRow(stringResource(R.string.set_show_unsure), stringResource(R.string.set_show_unsure_sub), overlay.showUnsure) { on ->
                vm.updateOverlay { it.copy(showUnsure = on) }
            }
        }

        PrCard {
            Label(stringResource(R.string.set_calibration_title))
            Text(stringResource(R.string.set_calibration_body), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
            SecondaryButton(stringResource(R.string.set_calibration_open), onClick = onOpenCalibration)
        }

        PrCard {
            Label(stringResource(R.string.perm_privacy_title))
            Text(stringResource(R.string.perm_privacy_body), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
            SecondaryButton(stringResource(R.string.set_clear_history), onClick = { confirmClear = true })
        }

        Text(
            stringResource(R.string.set_version, BuildConfig.VERSION_NAME),
            color = PrColors.Faint,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.set_clear_title)) },
            text = { Text(stringResource(R.string.set_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    confirmClear = false
                }) { Text(stringResource(R.string.set_clear_confirm), color = PrColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun stringResourceFor(style: IdleStyle): String = stringResource(
    when (style) {
        IdleStyle.RADAR -> R.string.style_radar
        IdleStyle.PULSE -> R.string.style_pulse
    },
)

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = PrColors.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = PrColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrColors.Accent, checkedThumbColor = PrColors.Bg),
        )
    }
}
