package ro.profitride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ro.profitride.R
import ro.profitride.core.Countries
import ro.profitride.core.Platform
import ro.profitride.core.Rating
import ro.profitride.data.RideRow
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

@Composable
fun RidesScreen(vm: MainViewModel) {
    val rides by vm.rides.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val cur = OverlayText.currency(profile)
    val unit = Countries.byCode(profile.countryCode).unit
    val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScreenTitle(stringResource(R.string.rides_title), stringResource(R.string.rides_sub))
                if (rides.isEmpty()) {
                    PrCard {
                        Text(stringResource(R.string.rides_empty), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
        items(rides, key = { it.id }) { ride ->
            RideRowCard(
                ride = ride,
                cur = cur,
                distance = Format.distance(ride.km, unit) + " " + unit.symbol,
                whenText = timeFormat.format(Date(ride.timestamp)),
                onToggle = { vm.setAccepted(ride.id, it) },
            )
        }
    }
}

@Composable
private fun RideRowCard(ride: RideRow, cur: String, distance: String, whenText: String, onToggle: (Boolean) -> Unit) {
    val color = when (ride.rating) {
        Rating.GOOD -> PrColors.Accent
        Rating.MEDIUM -> PrColors.Amber
        Rating.POOR -> PrColors.Red
    }
    val platformName = when (ride.platform) {
        Platform.BOLT -> "Bolt"
        Platform.UBER -> "Uber"
    }
    val toggleLabel = stringResource(R.string.rides_done_toggle)
    PrCard(border = if (ride.accepted) PrColors.Accent.copy(alpha = 0.35f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Dot(color, 12)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        Format.signed(ride.net, 1) + " " + cur,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrColors.Text,
                    )
                    Text(
                        "(" + Format.number(ride.price, 1) + " " + cur + ")",
                        fontSize = 13.sp,
                        color = PrColors.Muted,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Text(
                    "$platformName · $distance · ${ride.minutes.toInt()} min · $whenText",
                    fontSize = 12.sp,
                    color = PrColors.Muted,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = ride.accepted,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = toggleLabel },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PrColors.Accent,
                        checkedThumbColor = PrColors.Bg,
                    ),
                )
                Text(stringResource(R.string.rides_done_short), fontSize = 11.sp, color = PrColors.Muted)
            }
        }
    }
}
