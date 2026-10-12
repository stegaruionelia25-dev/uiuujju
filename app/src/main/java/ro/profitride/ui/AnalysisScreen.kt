package ro.profitride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ro.profitride.R
import ro.profitride.core.Analysis
import ro.profitride.core.Countries
import ro.profitride.core.Period
import ro.profitride.overlay.OverlayText
import ro.profitride.util.Format

/** Unde se duc banii și ce rămâne, pe azi, săptămână, lună sau an. Doar din cursele marcate ca făcute și veniturile adăugate. */
@Composable
fun AnalysisScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val rides by vm.analysisRides.collectAsStateWithLifecycle()
    val manual by vm.manual.collectAsStateWithLifecycle()
    var periodIndex by rememberSaveable { mutableIntStateOf(Period.WEEK.ordinal) }
    val period = Period.entries[periodIndex]
    val cur = OverlayText.currency(profile)
    val unit = Countries.byCode(profile.countryCode).unit
    val now = System.currentTimeMillis()
    val summary = remember(rides, manual, profile, period) { Analysis.summarize(rides, manual, profile, period, now) }
    val start = remember(period) { Analysis.periodStart(period, now) }
    val periodManual = manual.filter { it.timestampMillis >= start }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.analysis_title), stringResource(R.string.analysis_sub))
        ChipRow(
            options = Period.entries,
            selected = period,
            label = { periodLabel(it) },
            onSelect = { periodIndex = it.ordinal },
        )

        PrCard {
            Label(stringResource(R.string.analysis_profit))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Format.signed(summary.profit, 2), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
                Text(" $cur", fontSize = 18.sp, color = PrColors.Muted, modifier = Modifier.padding(bottom = 7.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.analysis_rides, summary.rides), color = PrColors.Muted, fontSize = 14.sp)
                Text(
                    Format.distance(summary.km, unit) + " " + unit.symbol,
                    color = PrColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrCard(Modifier.weight(1f)) {
                Label(stringResource(R.string.analysis_income))
                Text(Format.number(summary.income, 2) + " " + cur, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrColors.Accent)
                Hint(stringResource(R.string.analysis_income_sub))
            }
            PrCard(Modifier.weight(1f)) {
                Label(stringResource(R.string.analysis_expenses))
                Text(Format.number(summary.expenses, 2) + " " + cur, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrColors.Red)
                Hint(stringResource(R.string.analysis_expenses_sub))
            }
        }

        PrCard {
            Label(stringResource(R.string.analysis_where))
            if (summary.income > 0) {
                BreakdownRow(PrColors.Amber, stringResource(R.string.analysis_commissions), summary.commissions, cur)
                BreakdownRow(PrColors.Accent, stringResource(R.string.analysis_energy_wear), summary.energyAndWear, cur)
                BreakdownRow(PrColors.Purple, stringResource(R.string.legend_fixed), summary.fixed, cur)
                BreakdownRow(PrColors.Blue, stringResource(R.string.detail_tax), summary.tax, cur)
            } else {
                Text(stringResource(R.string.analysis_empty), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }

        PrCard {
            Label(stringResource(R.string.analysis_chart))
            BarChart(summary.buckets)
        }

        PrCard {
            Label(stringResource(R.string.manual_title))
            Hint(stringResource(R.string.manual_sub))
            var amount by remember { mutableStateOf(0.0) }
            var note by rememberSaveable { mutableStateOf("") }
            var resetKey by remember { mutableIntStateOf(0) }
            key(resetKey) {
                NumberField(stringResource(R.string.manual_amount), amount, { amount = it }, suffix = cur)
            }
            TextInput(stringResource(R.string.manual_note), note, { note = it }, hint = stringResource(R.string.manual_note_hint))
            PrimaryButton(
                stringResource(R.string.manual_add),
                onClick = {
                    vm.addManualIncome(amount, note)
                    amount = 0.0
                    note = ""
                    resetKey += 1
                },
                enabled = amount > 0,
            )
            val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            periodManual.take(8).forEach { m ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("+" + Format.number(m.amount, 2) + " " + cur, color = PrColors.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            dateFormat.format(Date(m.timestampMillis)) + if (m.note.isNotEmpty()) " · " + m.note else "",
                            color = PrColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                    TextButton(onClick = { vm.deleteManual(m.id) }) {
                        Text(stringResource(R.string.cal_remove), color = PrColors.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun periodLabel(p: Period): String = stringResource(
    when (p) {
        Period.DAY -> R.string.period_day
        Period.WEEK -> R.string.period_week
        Period.MONTH -> R.string.period_month
        Period.YEAR -> R.string.period_year
    },
)

@Composable
private fun BreakdownRow(color: Color, label: String, value: Double, cur: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Text(label, Modifier.padding(start = 10.dp).weight(1f), color = PrColors.Text, fontSize = 14.sp)
        Text(Format.number(value, 2) + " " + cur, color = PrColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
