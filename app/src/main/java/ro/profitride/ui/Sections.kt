package ro.profitride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.compose.runtime.saveable.rememberSaveable
import ro.profitride.core.CarCatalog
import ro.profitride.R
import ro.profitride.core.Countries
import ro.profitride.core.DriverProfile
import ro.profitride.core.FuelType
import ro.profitride.core.ProfitCalculator
import ro.profitride.core.VerdictBasis
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

typealias ProfileUpdate = ((DriverProfile) -> DriverProfile) -> Unit

@Composable
fun CountrySection(profile: DriverProfile, update: ProfileUpdate) {
    var expanded by remember { mutableStateOf(false) }
    val country = Countries.byCode(profile.countryCode)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(stringResource(R.string.country))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    Format.countryName(country),
                    modifier = Modifier.weight(1f),
                    color = PrColors.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Countries.all.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(Format.countryName(c)) },
                        onClick = {
                            update { it.copy(countryCode = c.code) }
                            expanded = false
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(stringResource(R.string.currency_pill, Format.currencySymbol(country.currency)), PrColors.Accent)
            StatusPill(stringResource(R.string.unit_pill, country.unit.symbol), PrColors.Accent)
        }
        Hint(stringResource(R.string.country_hint))
    }
}

@Composable
fun CarSection(profile: DriverProfile, update: ProfileUpdate) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val knownMake = CarCatalog.models.containsKey(profile.makeName)
        var customMake by rememberSaveable { mutableStateOf(profile.makeName.isNotBlank() && !knownMake) }
        val modelList = CarCatalog.modelsOf(profile.makeName)
        var customModel by rememberSaveable { mutableStateOf(profile.modelName.isNotBlank() && profile.modelName !in modelList) }
        val other = stringResource(R.string.car_other)

        PickerField(
            label = stringResource(R.string.car_make),
            value = if (customMake) other else profile.makeName,
            options = CarCatalog.makes + other,
            hint = stringResource(R.string.car_pick),
            onSelect = { v ->
                if (v == other) {
                    customMake = true
                    customModel = true
                    update { it.copy(makeName = "", modelName = "") }
                } else {
                    customMake = false
                    customModel = false
                    update { it.copy(makeName = v, modelName = "") }
                }
            },
        )
        if (customMake) {
            TextInput(
                label = stringResource(R.string.car_make_custom),
                value = profile.makeName,
                onValueChange = { v -> update { it.copy(makeName = v) } },
                hint = stringResource(R.string.car_make_hint),
                capitalization = KeyboardCapitalization.Words,
            )
        }
        if (customMake || customModel) {
            TextInput(
                label = stringResource(R.string.car_model),
                value = profile.modelName,
                onValueChange = { v -> update { it.copy(modelName = v) } },
                hint = stringResource(R.string.car_model_hint),
                capitalization = KeyboardCapitalization.Words,
            )
        } else {
            PickerField(
                label = stringResource(R.string.car_model),
                value = profile.modelName,
                options = modelList + other,
                hint = stringResource(R.string.car_pick),
                enabled = profile.makeName.isNotBlank(),
                onSelect = { v ->
                    if (v == other) {
                        customModel = true
                        update { it.copy(modelName = "") }
                    } else {
                        update { it.copy(modelName = v) }
                    }
                },
            )
        }
        PickerField(
            label = stringResource(R.string.car_year),
            value = if (profile.year > 0) profile.year.toString() else "",
            options = CarCatalog.years(Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() },
            hint = stringResource(R.string.car_pick),
            onSelect = { v -> update { it.copy(year = v.toIntOrNull() ?: 0) } },
        )
        Label(stringResource(R.string.car_fuel))
        ChipRow(
            options = FuelType.entries,
            selected = profile.fuelType,
            label = { fuelLabel(it) },
            onSelect = { f -> update { it.copy(fuelType = f) } },
        )
        if (profile.fuelType == FuelType.LPG || profile.fuelType == FuelType.HYBRID) {
            Hint(stringResource(R.string.fuel_blend_hint))
        }
    }
}

@Composable
fun fuelLabel(fuel: FuelType): String = stringResource(
    when (fuel) {
        FuelType.PETROL -> R.string.fuel_petrol
        FuelType.DIESEL -> R.string.fuel_diesel
        FuelType.LPG -> R.string.fuel_lpg
        FuelType.HYBRID -> R.string.fuel_hybrid
        FuelType.ELECTRIC -> R.string.fuel_electric
    },
)

@Composable
fun CostSection(profile: DriverProfile, update: ProfileUpdate) {
    val cur = OverlayText.currency(profile)
    val electric = profile.fuelType.perKwh
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = stringResource(R.string.cost_consumption),
                value = profile.consumption,
                onValueChange = { v -> update { it.copy(consumption = v) } },
                modifier = Modifier.weight(1f),
                suffix = if (electric) "kWh/100 km" else "l/100 km",
                hint = if (electric) "16" else "6,8",
            )
            NumberField(
                label = stringResource(R.string.cost_energy_price),
                value = profile.energyPrice,
                onValueChange = { v -> update { it.copy(energyPrice = v) } },
                modifier = Modifier.weight(1f),
                suffix = if (electric) "$cur/kWh" else "$cur/l",
                hint = if (electric) "1,2" else "7,4",
            )
        }
        Label(stringResource(R.string.cost_fixed))
        val month = stringResource(R.string.per_month)
        val year = stringResource(R.string.per_year)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = stringResource(R.string.fixed_rent),
                value = profile.rentMonthly,
                onValueChange = { v -> update { it.copy(rentMonthly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/$month",
            )
            NumberField(
                label = stringResource(R.string.fixed_accounting),
                value = profile.accountingMonthly,
                onValueChange = { v -> update { it.copy(accountingMonthly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/$month",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = stringResource(R.string.fixed_rca),
                value = profile.rcaYearly,
                onValueChange = { v -> update { it.copy(rcaYearly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/$year",
            )
            NumberField(
                label = stringResource(R.string.fixed_casco),
                value = profile.cascoYearly,
                onValueChange = { v -> update { it.copy(cascoYearly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/$year",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = stringResource(R.string.fixed_subscriptions),
                value = profile.subscriptionsMonthly,
                onValueChange = { v -> update { it.copy(subscriptionsMonthly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/$month",
            )
            NumberField(
                label = stringResource(R.string.fixed_taxes),
                value = profile.taxesYearly,
                onValueChange = { v -> update { it.copy(taxesYearly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/$year",
            )
        }
        NumberField(
            label = stringResource(R.string.fixed_other),
            value = profile.yearlyFixedCosts,
            onValueChange = { v -> update { it.copy(yearlyFixedCosts = v) } },
            suffix = "$cur/$year",
        )
        Hint(stringResource(R.string.cost_fixed_hint))
        NumberField(
            label = stringResource(R.string.cost_wear),
            value = profile.yearlyWearCosts,
            onValueChange = { v -> update { it.copy(yearlyWearCosts = v) } },
            suffix = "$cur/${stringResource(R.string.per_year)}",
            hint = "5600",
        )
        Hint(stringResource(R.string.cost_wear_hint))
        NumberField(
            label = stringResource(R.string.cost_yearly_km),
            value = profile.yearlyKm,
            onValueChange = { v -> update { it.copy(yearlyKm = v) } },
            suffix = "km/${stringResource(R.string.per_year)}",
            hint = "40000",
            integersOnly = true,
        )
    }
}

@Composable
fun CostPerKmCard(profile: DriverProfile) {
    val ready = profile.consumption > 0 && profile.energyPrice > 0 && profile.yearlyKm > 0
    val country = Countries.byCode(profile.countryCode)
    val cur = OverlayText.currency(profile)
    PrCard(border = if (ready) PrColors.Accent.copy(alpha = 0.35f) else null) {
        Label(stringResource(R.string.cost_per_km_title, country.unit.symbol))
        if (!ready) {
            Text(stringResource(R.string.cost_per_km_empty), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
            return@PrCard
        }
        val cpk = ProfitCalculator.costPerKm(profile)
        val k = country.unit.kmPerUnit
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Format.number(cpk.total * k, 2), fontSize = 44.sp, fontWeight = FontWeight.Black, color = PrColors.Hi)
            Text(
                " $cur/${country.unit.symbol}",
                fontSize = 17.sp,
                color = PrColors.Muted,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
        val total = cpk.total.coerceAtLeast(0.0001)
        Row(
            Modifier
                .fillMaxWidth()
                .height(12.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            listOf(cpk.energy to PrColors.Accent, cpk.fixed to PrColors.Purple, cpk.wear to PrColors.Blue).forEach { (value, color) ->
                val w = (value / total).toFloat().coerceAtLeast(0.001f)
                Box(
                    Modifier
                        .weight(w)
                        .height(12.dp)
                        .background(color, RoundedCornerShape(6.dp)),
                )
            }
        }
        LegendRow(PrColors.Accent, stringResource(R.string.legend_energy), Format.number(cpk.energy * k, 2))
        LegendRow(PrColors.Blue, stringResource(R.string.legend_wear), Format.number(cpk.wear * k, 2))
        LegendRow(PrColors.Purple, stringResource(R.string.legend_fixed), Format.number(cpk.fixed * k, 2))
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        Text(label, Modifier.padding(start = 10.dp).weight(1f), color = PrColors.Text, fontSize = 14.sp)
        Text(value, color = PrColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EarningsSection(profile: DriverProfile, update: ProfileUpdate) {
    val cur = OverlayText.currency(profile)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = stringResource(R.string.comm_bolt),
                value = profile.commissionBoltPct,
                onValueChange = { v -> update { it.copy(commissionBoltPct = v.coerceAtMost(100.0)) } },
                modifier = Modifier.weight(1f),
                suffix = "%",
                hint = "20",
            )
            NumberField(
                label = stringResource(R.string.comm_uber),
                value = profile.commissionUberPct,
                onValueChange = { v -> update { it.copy(commissionUberPct = v.coerceAtMost(100.0)) } },
                modifier = Modifier.weight(1f),
                suffix = "%",
                hint = "20",
            )
        }
        Hint(stringResource(R.string.comm_hint))
        Label(stringResource(R.string.fleet_title))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = stringResource(R.string.fleet_pct),
                value = profile.fleetPct,
                onValueChange = { v -> update { it.copy(fleetPct = v.coerceAtMost(100.0)) } },
                modifier = Modifier.weight(1f),
                suffix = "%",
            )
            NumberField(
                label = stringResource(R.string.fleet_fixed),
                value = profile.fleetFixedMonthly,
                onValueChange = { v -> update { it.copy(fleetFixedMonthly = v) } },
                modifier = Modifier.weight(1f),
                suffix = "$cur/${stringResource(R.string.per_month)}",
            )
        }
        Hint(stringResource(R.string.fleet_hint))
        NumberField(
            label = stringResource(R.string.tax_reserve),
            value = profile.taxReservePct,
            onValueChange = { v -> update { it.copy(taxReservePct = v.coerceAtMost(100.0)) } },
            suffix = stringResource(R.string.tax_reserve_suffix),
            hint = "10",
        )
        Hint(stringResource(R.string.tax_hint))

        Label(stringResource(R.string.verdict_basis))
        ChipRow(
            options = VerdictBasis.entries,
            selected = profile.basis,
            label = { basisChipLabel(it, profile) },
            onSelect = { b -> update { it.copy(basis = b) } },
        )
        NumberField(
            label = stringResource(R.string.good_threshold),
            value = profile.goodThreshold,
            onValueChange = { v -> update { it.copy(goodThreshold = v) } },
            suffix = OverlayText.basisUnit(LocalContext.current, profile.basis, profile),
            hint = when (profile.basis) {
                VerdictBasis.PER_KM -> "1,2"
                VerdictBasis.PER_HOUR -> "40"
                VerdictBasis.NET -> "15"
            },
        )
        ThresholdPreview(profile)
        NumberField(
            label = stringResource(R.string.daily_target),
            value = profile.dailyTarget,
            onValueChange = { v -> update { it.copy(dailyTarget = v) } },
            suffix = cur,
            hint = "250",
        )
    }
}

@Composable
internal fun basisChipLabel(basis: VerdictBasis, profile: DriverProfile): String {
    val cur = OverlayText.currency(profile)
    val unit = Countries.byCode(profile.countryCode).unit.symbol
    return when (basis) {
        VerdictBasis.PER_KM -> "$cur/$unit"
        VerdictBasis.PER_HOUR -> "$cur/h"
        VerdictBasis.NET -> stringResource(R.string.basis_net_chip, cur)
    }
}

@Composable
private fun ThresholdPreview(profile: DriverProfile) {
    if (profile.goodThreshold <= 0) return
    val digits = if (profile.basis == VerdictBasis.PER_KM) 2 else 0
    val good = profile.goodThreshold
    val mid = good * profile.mediumFraction
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThresholdPill(PrColors.Accent, "> ${Format.number(good, digits)}", Modifier.weight(1f))
        ThresholdPill(PrColors.Amber, "${Format.number(mid, digits)} – ${Format.number(good, digits)}", Modifier.weight(1f))
        ThresholdPill(PrColors.Red, "< ${Format.number(mid, digits)}", Modifier.weight(1f))
    }
}

@Composable
private fun ThresholdPill(color: Color, text: String, modifier: Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
