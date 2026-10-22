package ro.profitride.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.profitride.R
import ro.profitride.core.CostMath
import ro.profitride.core.CostPeriod
import ro.profitride.core.Countries
import ro.profitride.core.DriverProfile
import ro.profitride.core.FuelType
import ro.profitride.core.VerdictBasis
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

/** Rândurile ecranului „Costurile mele”. Fiecare are titlu, explicație la „i” și, uneori, un exemplu. */
internal enum class CostId(@StringRes val title: Int, @StringRes val info: Int, @StringRes val example: Int? = null) {
    COUNTRY(R.string.ct_country, R.string.ct_country_info),
    CAR(R.string.ct_car, R.string.ct_car_info),
    FUEL(R.string.ct_fuel, R.string.ct_fuel_info),
    CONSUMPTION(R.string.ct_consumption, R.string.ct_consumption_info, R.string.ct_consumption_ex),
    PRICE(R.string.ct_price, R.string.ct_price_info, R.string.ct_price_ex),
    KM(R.string.ct_km, R.string.ct_km_info, R.string.ct_km_ex),
    RCA(R.string.ct_rca, R.string.ct_rca_info, R.string.ct_rca_ex),
    CASCO(R.string.ct_casco, R.string.ct_casco_info),
    ITP(R.string.ct_itp, R.string.ct_itp_info, R.string.ct_itp_ex),
    VIGNETTE(R.string.ct_vignette, R.string.ct_vignette_info),
    TAXES(R.string.ct_taxes, R.string.ct_taxes_info),
    OTHER(R.string.ct_other, R.string.ct_other_info),
    WEAR(R.string.ct_wear, R.string.ct_wear_info, R.string.ct_wear_ex),
    RENT(R.string.ct_rent, R.string.ct_rent_info),
    ACCOUNTING(R.string.ct_accounting, R.string.ct_accounting_info),
    SUBS(R.string.ct_subs, R.string.ct_subs_info),
    BOLT(R.string.ct_bolt, R.string.ct_bolt_info, R.string.ct_bolt_ex),
    UBER(R.string.ct_uber, R.string.ct_uber_info, R.string.ct_uber_ex),
    FLEET_PCT(R.string.ct_fleetpct, R.string.ct_fleetpct_info),
    FLEET_FIX(R.string.ct_fleetfix, R.string.ct_fleetfix_info),
    TAX_RES(R.string.ct_taxres, R.string.ct_taxres_info, R.string.ct_taxres_ex),
    BASIS(R.string.ct_basis, R.string.ct_basis_info),
    THRESHOLD(R.string.ct_threshold, R.string.ct_threshold_info, R.string.ct_threshold_ex),
    DAILY(R.string.ct_daily, R.string.ct_daily_info),
    TOUR(R.string.ct_tour, R.string.ct_tour_info),
}

/** Valoarea afișată în dreapta rândului și dacă lipsește ceva obligatoriu (se arată cu galben). */
@Composable
internal fun costValue(id: CostId, p: DriverProfile): Pair<String, Boolean> {
    val country = Countries.byCode(p.countryCode)
    val cur = OverlayText.currency(p)
    val yr = stringResource(R.string.per_year)
    val mo = stringResource(R.string.per_month)
    val fill = stringResource(R.string.ct_not_filled)
    val none = stringResource(R.string.ct_none)
    val context = LocalContext.current
    val electric = p.fuelType.perKwh
    fun money(v: Double, per: String) = if (v > 0) Format.number(v, 0) + " " + cur + "/" + per else none
    fun pct(v: Double) = if (v > 0) Format.plain(v) + " %" else none
    return when (id) {
        CostId.COUNTRY -> Format.countryName(country) + " · " + cur + ", " + country.unit.symbol to false
        CostId.CAR -> {
            val name = (p.makeName + " " + p.modelName).trim()
            if (name.isEmpty()) fill to false else name to false
        }
        CostId.FUEL -> fuelLabel(p.fuelType) to false
        CostId.CONSUMPTION ->
            if (p.consumption > 0) Format.plain(p.consumption) + " " + (if (electric) "kWh/100 km" else "l/100 km") to false
            else fill to true
        CostId.PRICE ->
            if (p.energyPrice > 0) Format.plain(p.energyPrice) + " " + cur + "/" + (if (electric) "kWh" else "l") to false
            else fill to true
        CostId.KM -> if (p.yearlyKm > 0) Format.number(p.yearlyKm, 0) + " km/" + yr to false else fill to true
        CostId.RCA -> money(p.rcaYearly, yr) to false
        CostId.CASCO -> money(p.cascoYearly, yr) to false
        CostId.ITP -> money(p.itpYearly, yr) to false
        CostId.VIGNETTE -> money(p.vignetteYearly, yr) to false
        CostId.TAXES -> money(p.taxesYearly, yr) to false
        CostId.OTHER -> money(p.yearlyFixedCosts, yr) to false
        CostId.WEAR -> money(p.yearlyWearCosts, yr) to false
        CostId.RENT -> money(p.rentMonthly, mo) to false
        CostId.ACCOUNTING -> money(p.accountingMonthly, mo) to false
        CostId.SUBS -> money(p.subscriptionsMonthly, mo) to false
        CostId.BOLT -> pct(p.commissionBoltPct) to false
        CostId.UBER -> pct(p.commissionUberPct) to false
        CostId.FLEET_PCT -> pct(p.fleetPct) to false
        CostId.FLEET_FIX -> money(p.fleetFixedMonthly, mo) to false
        CostId.TAX_RES -> pct(p.taxReservePct) to false
        CostId.BASIS -> basisChipLabel(p.basis, p) to false
        CostId.THRESHOLD ->
            if (p.goodThreshold > 0) Format.plain(p.goodThreshold) + " " + OverlayText.basisUnit(context, p.basis, p) to false
            else fill to true
        CostId.DAILY -> if (p.dailyTarget > 0) Format.number(p.dailyTarget, 0) + " " + cur to false else none to false
        CostId.TOUR -> stringResource(R.string.ct_tour_value) to false
    }
}

private class NumSpec(
    val get: (DriverProfile) -> Double,
    val set: (DriverProfile, Double) -> DriverProfile,
    /** perioada în care e salvată suma; null = nu e o sumă pe perioadă */
    val stored: CostPeriod? = null,
    val integers: Boolean = false,
    val max: Double = 1_000_000.0,
    val affectsCost: Boolean = true,
)

private fun numSpec(id: CostId): NumSpec? = when (id) {
    CostId.CONSUMPTION -> NumSpec({ it.consumption }, { p, v -> p.copy(consumption = v) }, max = 100.0)
    CostId.PRICE -> NumSpec({ it.energyPrice }, { p, v -> p.copy(energyPrice = v) }, max = 1000.0)
    CostId.KM -> NumSpec({ it.yearlyKm }, { p, v -> p.copy(yearlyKm = v) }, integers = true, max = 500_000.0)
    CostId.RCA -> NumSpec({ it.rcaYearly }, { p, v -> p.copy(rcaYearly = v) }, CostPeriod.YEAR)
    CostId.CASCO -> NumSpec({ it.cascoYearly }, { p, v -> p.copy(cascoYearly = v) }, CostPeriod.YEAR)
    CostId.ITP -> NumSpec({ it.itpYearly }, { p, v -> p.copy(itpYearly = v) }, CostPeriod.YEAR)
    CostId.VIGNETTE -> NumSpec({ it.vignetteYearly }, { p, v -> p.copy(vignetteYearly = v) }, CostPeriod.YEAR)
    CostId.TAXES -> NumSpec({ it.taxesYearly }, { p, v -> p.copy(taxesYearly = v) }, CostPeriod.YEAR)
    CostId.OTHER -> NumSpec({ it.yearlyFixedCosts }, { p, v -> p.copy(yearlyFixedCosts = v) }, CostPeriod.YEAR)
    CostId.WEAR -> NumSpec({ it.yearlyWearCosts }, { p, v -> p.copy(yearlyWearCosts = v) }, CostPeriod.YEAR)
    CostId.RENT -> NumSpec({ it.rentMonthly }, { p, v -> p.copy(rentMonthly = v) }, CostPeriod.MONTH)
    CostId.ACCOUNTING -> NumSpec({ it.accountingMonthly }, { p, v -> p.copy(accountingMonthly = v) }, CostPeriod.MONTH)
    CostId.SUBS -> NumSpec({ it.subscriptionsMonthly }, { p, v -> p.copy(subscriptionsMonthly = v) }, CostPeriod.MONTH)
    CostId.FLEET_FIX -> NumSpec({ it.fleetFixedMonthly }, { p, v -> p.copy(fleetFixedMonthly = v) }, CostPeriod.MONTH)
    CostId.BOLT -> NumSpec({ it.commissionBoltPct }, { p, v -> p.copy(commissionBoltPct = v) }, max = 100.0, affectsCost = false)
    CostId.UBER -> NumSpec({ it.commissionUberPct }, { p, v -> p.copy(commissionUberPct = v) }, max = 100.0, affectsCost = false)
    CostId.FLEET_PCT -> NumSpec({ it.fleetPct }, { p, v -> p.copy(fleetPct = v) }, max = 100.0, affectsCost = false)
    CostId.TAX_RES -> NumSpec({ it.taxReservePct }, { p, v -> p.copy(taxReservePct = v) }, max = 100.0, affectsCost = false)
    CostId.THRESHOLD -> NumSpec({ it.goodThreshold }, { p, v -> p.copy(goodThreshold = v) }, affectsCost = false)
    CostId.DAILY -> NumSpec({ it.dailyTarget }, { p, v -> p.copy(dailyTarget = v) }, affectsCost = false)
    else -> null
}

private fun CostPeriod.other(): CostPeriod = if (this == CostPeriod.YEAR) CostPeriod.MONTH else CostPeriod.YEAR

/** Foaia de editare: scrii valoarea, vezi pe loc cât schimbă în costul pe km, apoi salvezi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CostEditSheet(id: CostId, profile: DriverProfile, update: ProfileUpdate, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetContainer,
        contentColor = SheetText,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        SheetTheme {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(id.title), fontSize = 26.sp, fontWeight = FontWeight.Black, color = PrColors.Text)
                when (id) {
                    CostId.COUNTRY -> {
                        CountrySection(profile, update)
                        PrimaryButton(stringResource(R.string.ct_done), onClose)
                    }
                    CostId.CAR -> {
                        CarSection(profile, update)
                        PrimaryButton(stringResource(R.string.ct_done), onClose)
                    }
                    CostId.FUEL -> {
                        ChipRow(
                            options = FuelType.entries,
                            selected = profile.fuelType,
                            label = { fuelLabel(it) },
                            onSelect = { f -> update { it.copy(fuelType = f) } },
                        )
                        Hint(stringResource(R.string.fuel_blend_hint))
                        PrimaryButton(stringResource(R.string.ct_done), onClose)
                    }
                    CostId.BASIS -> {
                        ChipRow(
                            options = VerdictBasis.entries,
                            selected = profile.basis,
                            label = { basisChipLabel(it, profile) },
                            onSelect = { b -> update { it.copy(basis = b) } },
                        )
                        Hint(stringResource(R.string.ct_basis_warn))
                        PrimaryButton(stringResource(R.string.ct_done), onClose)
                    }
                    else -> NumEditor(id, profile, update, onClose)
                }
            }
        }
    }
}

@Composable
private fun NumEditor(id: CostId, profile: DriverProfile, update: ProfileUpdate, onClose: () -> Unit) {
    val spec = numSpec(id) ?: return
    val context = LocalContext.current
    val country = Countries.byCode(profile.countryCode)
    val cur = OverlayText.currency(profile)
    val year = stringResource(R.string.per_year)
    val electric = profile.fuelType.perKwh
    val suffix = when (id) {
        CostId.CONSUMPTION -> if (electric) "kWh/100 km" else "l/100 km"
        CostId.PRICE -> if (electric) "$cur/kWh" else "$cur/l"
        CostId.KM -> "km/$year"
        CostId.BOLT, CostId.UBER, CostId.FLEET_PCT, CostId.TAX_RES -> "%"
        CostId.THRESHOLD -> OverlayText.basisUnit(context, profile.basis, profile)
        else -> cur
    }
    val stored = spec.stored
    var other by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable { mutableStateOf(spec.get(profile)) }
    val inputPeriod = stored?.let { if (other) it.other() else it }

    key(other) {
        NumberField(
            label = stringResource(id.title),
            value = typed,
            onValueChange = { typed = it.coerceAtMost(spec.max) },
            suffix = suffix,
            integersOnly = spec.integers,
        )
    }

    if (stored != null && inputPeriod != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false, true).forEach { wantOther ->
                val period = if (wantOther) stored.other() else stored
                val on = wantOther == other
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(if (on) PrColors.Brand else PrColors.Surface, RoundedCornerShape(999.dp))
                        .clickable {
                            if (wantOther != other) {
                                typed = CostMath.round2(CostMath.convert(typed, inputPeriod, period))
                                other = wantOther
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (period == CostPeriod.YEAR) R.string.ct_per_year else R.string.ct_per_month),
                        color = if (on) PrColors.OnBrand else PrColors.Faint,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }

    val storedValue = if (stored != null && inputPeriod != null) CostMath.convert(typed, inputPeriod, stored) else typed

    if (spec.affectsCost) {
        val perKm = CostMath.totalPerKm(spec.set(profile, storedValue))
        Box(
            Modifier
                .fillMaxWidth()
                .background(if (perKm != null) PrColors.Accent.copy(alpha = 0.12f) else PrColors.Surface, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                if (perKm != null) {
                    stringResource(R.string.ct_effect, Format.number(perKm * country.unit.kmPerUnit, 2), cur, country.unit.symbol)
                } else {
                    stringResource(R.string.ct_effect_empty)
                },
                color = if (perKm != null) PrColors.Accent else PrColors.Muted,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 20.sp,
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        SecondaryButton(stringResource(R.string.ct_dismiss), onClick = onClose, modifier = Modifier.weight(0.7f))
        PrimaryButton(
            stringResource(R.string.ct_save),
            onClick = {
                update { spec.set(it, storedValue) }
                onClose()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Foaia de la butonul „i”: ce înseamnă opțiunea, un exemplu și „Am înțeles”. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CostInfoSheet(id: CostId, currency: String, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetContainer,
        contentColor = SheetText,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        SheetTheme {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(PrColors.Brand, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("i", color = PrColors.OnBrand, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Text(stringResource(id.title), fontSize = 26.sp, fontWeight = FontWeight.Black, color = PrColors.Text)
                }
                Text(
                    stringResource(id.info),
                    color = PrColors.Muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                )
                id.example?.let { ex ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(PrColors.Surface, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.ct_example).uppercase(),
                            color = PrColors.Hi,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            stringResource(ex, currency),
                            color = PrColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 21.sp,
                        )
                    }
                }
                PrimaryButton(stringResource(R.string.ct_got_it), onClose)
            }
        }
    }
}
