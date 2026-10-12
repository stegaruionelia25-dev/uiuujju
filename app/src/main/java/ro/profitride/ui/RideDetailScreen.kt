package ro.profitride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ro.profitride.R
import ro.profitride.core.Countries
import ro.profitride.core.Platform
import ro.profitride.core.ProfitCalculator
import ro.profitride.core.Rating
import ro.profitride.core.RideOffer
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

/** Detaliile unei oferte salvate: de unde vine profitul, plus corectarea manuală dacă citirea a greșit. */
@Composable
fun RideDetailScreen(vm: MainViewModel, rideId: Long, onBack: () -> Unit) {
    val rides by vm.rides.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val ride = rides.firstOrNull { it.id == rideId }
    var editing by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .size(44.dp)
                    .background(PrColors.Surface, RoundedCornerShape(14.dp))
                    .clickable(onClickLabel = stringResource(R.string.back), onClick = onBack),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = PrColors.Text)
            }
            Text(stringResource(R.string.detail_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
        }

        if (ride == null) {
            PrCard { Text(stringResource(R.string.detail_missing), color = PrColors.Muted, fontSize = 14.sp) }
            return@Column
        }

        val country = Countries.byCode(profile.countryCode)
        val cur = OverlayText.currency(profile)
        val unit = country.unit
        val v = ProfitCalculator.evaluate(RideOffer(ride.platform, ride.price, 0.0, ride.km, 0.0, ride.minutes, ride.priceNet), profile)
        val cpk = ProfitCalculator.costPerKm(profile)
        val color = when (v.rating) {
            Rating.GOOD -> PrColors.Accent
            Rating.MEDIUM -> PrColors.Amber
            Rating.POOR -> PrColors.Red
        }
        val platformName = if (ride.platform == Platform.BOLT) "Bolt" else "Uber"
        val whenText = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ride.timestamp))

        PrCard(border = color.copy(alpha = 0.45f)) {
            Label("$platformName · $whenText")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Format.signed(v.net, 2), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
                Text(" $cur", fontSize = 18.sp, color = PrColors.Muted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                OverlayText.ratingLabel(LocalContext.current, v.rating),
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
            )
            Text(
                Format.distance(ride.km, unit) + " " + unit.symbol + " · " + ride.minutes.toInt() + " min",
                color = PrColors.Muted,
                fontSize = 14.sp,
            )
        }

        PrCard {
            KeyValueRow(stringResource(R.string.detail_price), Format.number(ride.price, 2) + " " + cur)
            if (v.commission > 0) KeyValueRow(stringResource(R.string.detail_commission), "−" + Format.number(v.commission, 2) + " " + cur)
            if (v.fleetFee > 0) KeyValueRow(stringResource(R.string.detail_fleet), "−" + Format.number(v.fleetFee, 2) + " " + cur)
            KeyValueRow(stringResource(R.string.legend_energy), "−" + Format.number(ride.km * cpk.energy, 2) + " " + cur)
            KeyValueRow(stringResource(R.string.legend_wear), "−" + Format.number(ride.km * cpk.wear, 2) + " " + cur)
            KeyValueRow(stringResource(R.string.legend_fixed), "−" + Format.number(ride.km * cpk.fixed, 2) + " " + cur)
            if (v.taxReserve > 0) KeyValueRow(stringResource(R.string.detail_tax), "−" + Format.number(v.taxReserve, 2) + " " + cur)
            KeyValueRow(stringResource(R.string.detail_net), Format.signed(v.net, 2) + " " + cur, color)
            KeyValueRow(
                stringResource(R.string.detail_per_unit, unit.symbol),
                Format.number(v.perKm * unit.kmPerUnit, 2) + " " + cur + "/" + unit.symbol,
            )
            KeyValueRow(stringResource(R.string.detail_per_hour), Format.number(v.perHour, 0) + " " + cur + "/h")
            Hint(stringResource(R.string.detail_hint))
        }

        SecondaryButton(stringResource(R.string.detail_edit), onClick = { editing = true })

        PrCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.rides_done_toggle), Modifier.weight(1f), color = PrColors.Text, fontWeight = FontWeight.Bold)
                Switch(
                    checked = ride.accepted,
                    onCheckedChange = { vm.setAccepted(ride.id, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrColors.Accent, checkedThumbColor = PrColors.Bg),
                )
            }
        }

        if (editing) {
            var price by remember { mutableDoubleStateOf(ride.price) }
            var km by remember { mutableDoubleStateOf(ride.km) }
            var minutes by remember { mutableDoubleStateOf(ride.minutes) }
            AlertDialog(
                onDismissRequest = { editing = false },
                title = { Text(stringResource(R.string.detail_edit)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Hint(stringResource(R.string.detail_edit_hint))
                        NumberField(stringResource(R.string.detail_price), price, { price = it }, suffix = cur)
                        NumberField(stringResource(R.string.detail_total_km, unit.symbol), km / unit.kmPerUnit, { km = it * unit.kmPerUnit }, suffix = unit.symbol)
                        NumberField(stringResource(R.string.detail_total_min), minutes, { minutes = it }, suffix = "min")
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = price > 0 && km > 0 && minutes > 0,
                        onClick = {
                            vm.editRide(ride, price, km, minutes)
                            editing = false
                        },
                    ) { Text(stringResource(R.string.detail_save), color = PrColors.Accent) }
                },
                dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}
