package ro.profitride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar
import ro.profitride.R
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

@Composable
fun HomeScreen(vm: MainViewModel, onOpenRides: () -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val serviceEnabled by vm.serviceEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cur = OverlayText.currency(profile)

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greet_morning
            hour < 18 -> R.string.greet_day
            else -> R.string.greet_evening
        },
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(greeting)

        if (serviceEnabled) {
            PrCard(border = PrColors.Accent.copy(alpha = 0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dot(PrColors.Accent)
                    Text(stringResource(R.string.home_overlay_on), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                }
                Text(stringResource(R.string.home_overlay_on_sub), color = PrColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        } else {
            PrCard(border = PrColors.Amber.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dot(PrColors.Amber)
                    Text(stringResource(R.string.home_overlay_off), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                }
                Text(stringResource(R.string.home_overlay_off_sub), color = PrColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
                PrimaryButton(
                    stringResource(R.string.perm_open_accessibility),
                    onClick = { AccessibilityStatus.openAccessibilitySettings(context) },
                )
            }
        }

        PrCard {
            Label(stringResource(R.string.home_today_net))
            if (stats.today.rides > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Format.number(stats.today.net, 1),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrColors.Text,
                    )
                    Text(" $cur", fontSize = 18.sp, color = PrColors.Muted, modifier = Modifier.padding(bottom = 8.dp))
                }
            } else {
                Text("—", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = PrColors.Faint)
                Text(
                    stringResource(R.string.home_today_empty),
                    color = PrColors.Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }

            if (profile.dailyTarget > 0) {
                val progress = (stats.today.net / profile.dailyTarget).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = PrColors.Accent,
                    trackColor = PrColors.Surface2,
                )
                val remaining = profile.dailyTarget - stats.today.net
                Text(
                    if (remaining > 0) {
                        stringResource(R.string.home_target_left, Format.number(remaining, 0) + " " + cur, Format.number(profile.dailyTarget, 0) + " " + cur)
                    } else {
                        stringResource(R.string.idle_target_reached)
                    },
                    color = PrColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(stats.today.rides.toString(), stringResource(R.string.home_rides_done))
                StatTile(stats.today.perHour?.let { Format.number(it, 0) } ?: "—", "$cur/h")
                StatTile(stats.offersToday.toString(), stringResource(R.string.home_offers_seen))
            }
            if (stats.today.rides == 0 && stats.offersToday > 0) {
                SecondaryButton(stringResource(R.string.home_mark_rides), onClick = onOpenRides)
            }
        }

        CostPerKmCard(profile)

        if (stats.bestHour != null) {
            PrCard(border = PrColors.Amber.copy(alpha = 0.4f)) {
                Label(stringResource(R.string.home_best_window))
                Text(
                    stringResource(R.string.idle_best_window, Format.hour(stats.bestHour ?: 0)),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrColors.Text,
                )
                Hint(stringResource(R.string.home_best_window_hint))
            }
        } else {
            PrCard {
                Label(stringResource(R.string.home_history_title))
                Text(stringResource(R.string.home_history_body), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}
