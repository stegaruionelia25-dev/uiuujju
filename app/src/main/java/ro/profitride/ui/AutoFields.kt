package ro.profitride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.profitride.R
import ro.profitride.core.AutoLogic
import ro.profitride.core.DateInput

/** Antet cu buton înapoi, folosit în toate ecranele din meniul Auto. */
@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .background(PrColors.Surface, RoundedCornerShape(14.dp))
                .clickable(onClickLabel = stringResource(R.string.back), onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = PrColors.Text)
        }
        Column {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrColors.Muted)
        }
    }
}

/** Căsuța verde care explică ce trebuie scris în formular. */
@Composable
fun TipCard(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PrColors.Accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = PrColors.Accent, modifier = Modifier.size(18.dp))
        Text(text, color = PrColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp)
    }
}

/**
 * Câmp pentru dată: șoferul scrie doar cifre, aplicația pune punctele și, dacă data nu există
 * (de exemplu 31.02), arată motivul și propune data corectă.
 */
@Composable
fun DateField(
    label: String,
    text: String,
    onText: (String) -> Unit,
    help: String,
    modifier: Modifier = Modifier,
) {
    val result = DateInput.validate(text)
    val error = result as? DateInput.Result.Invalid
    OutlinedTextField(
        value = text,
        onValueChange = { onText(DateInput.format(it)) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.auto_date_ph), color = PrColors.Faint) },
        supportingText = {
            Text(
                if (error != null) {
                    stringResource(
                        when (error.reason) {
                            DateInput.Reason.DAY -> R.string.auto_date_err_day
                            DateInput.Reason.MONTH -> R.string.auto_date_err_month
                            DateInput.Reason.DAY_OF_MONTH -> R.string.auto_date_err_dom
                            DateInput.Reason.YEAR -> R.string.auto_date_err_year
                        },
                    )
                } else {
                    help
                },
                color = if (error != null) PrColors.Red else PrColors.Faint,
                fontSize = 12.sp,
            )
        },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
    val suggestion = error?.suggestion
    if (suggestion != null) {
        val shown = DateInput.display(suggestion)
        Row(
            Modifier
                .fillMaxWidth()
                .background(PrColors.Red.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.auto_date_suggest, shown), color = PrColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.auto_date_fix),
                color = PrColors.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clickable { onText(shown) }
                    .padding(8.dp),
            )
        }
    }
}

/** Câmp pentru kilometri: doar cifre, cu punctul de mii pus automat (148200 devine 148.200). */
@Composable
fun KmField(
    label: String,
    value: Long,
    onValue: (Long) -> Unit,
    help: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = if (value > 0) AutoLogic.groupThousands(value) else "",
        onValueChange = { raw -> onValue(raw.filter { it.isDigit() }.take(7).toLongOrNull() ?: 0L) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text("km", color = PrColors.Muted, fontSize = 13.sp) },
        supportingText = { Text(help, color = PrColors.Faint, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
}

/** Alegere multiplă cu butoane (de exemplu zilele de reamintire). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChips(
    options: List<T>,
    selected: Set<T>,
    label: @Composable (T) -> String,
    onToggle: (T) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val on = option in selected
            FilterChip(
                selected = on,
                onClick = { onToggle(option) },
                label = { Text(label(option), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PrColors.Surface,
                    labelColor = PrColors.Muted,
                    selectedContainerColor = PrColors.Hi.copy(alpha = 0.16f),
                    selectedLabelColor = PrColors.Hi,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = on,
                    borderColor = PrColors.Outline,
                    selectedBorderColor = PrColors.Hi,
                ),
            )
        }
    }
}
