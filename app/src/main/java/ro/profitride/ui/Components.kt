package ro.profitride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.profitride.util.Format

@Composable
fun PrCard(
    modifier: Modifier = Modifier,
    border: Color? = null,
    container: Color = PrColors.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = container,
        border = border?.let { BorderStroke(1.dp, it) },
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
        if (subtitle != null) {
            Text(subtitle, fontSize = 14.sp, color = PrColors.Muted, lineHeight = 20.sp)
        }
    }
}

@Composable
fun Label(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrColors.Muted)
}

@Composable
fun Hint(text: String) {
    Text(text, fontSize = 12.sp, color = PrColors.Faint, lineHeight = 17.sp)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrColors.Accent,
            contentColor = PrColors.Bg,
            disabledContainerColor = PrColors.Surface2,
            disabledContentColor = PrColors.Faint,
        ),
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrColors.Outline),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
    }
}

/**
 * Câmp numeric care acceptă „6,8” și „6.8”. Goleste = 0 (necompletat).
 * Textul e păstrat local, ca să poți scrie liber „6,” fără să sară cursorul.
 */
@Composable
fun NumberField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    hint: String? = null,
    integersOnly: Boolean = false,
    help: String? = null,
) {
    var text by rememberSaveable(label) { mutableStateOf(if (value > 0) Format.plain(value) else "") }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val ok = raw.length <= 10 && raw.all { it.isDigit() || (!integersOnly && (it == ',' || it == '.')) }
            if (ok) {
                text = raw
                onValueChange(Format.parseInput(raw) ?: 0.0)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = hint?.let { { Text(it, color = PrColors.Faint) } },
        suffix = suffix?.let { { Text(it, color = PrColors.Muted, fontSize = 13.sp) } },
        supportingText = help?.let { { Text(it, color = PrColors.Faint, fontSize = 12.sp) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (integersOnly) KeyboardType.Number else KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
}

@Composable
fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    help: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 40) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = hint?.let { { Text(it, color = PrColors.Faint) } },
        supportingText = help?.let { { Text(it, color = PrColors.Faint, fontSize = 12.sp) } },
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrColors.Accent,
    unfocusedBorderColor = PrColors.Outline,
    focusedLabelColor = PrColors.Accent,
    unfocusedLabelColor = PrColors.Muted,
    cursorColor = PrColors.Accent,
    focusedContainerColor = PrColors.Surface,
    unfocusedContainerColor = PrColors.Surface,
)

/** Rând de chip-uri cu selecție unică, derulabil pe orizontală. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PrColors.Surface,
                    labelColor = PrColors.Muted,
                    selectedContainerColor = PrColors.Accent.copy(alpha = 0.14f),
                    selectedLabelColor = PrColors.Accent,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = option == selected,
                    borderColor = PrColors.Outline,
                    selectedBorderColor = PrColors.Accent,
                ),
            )
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(
        Modifier
            .size(size.dp)
            .background(color, CircleShape),
    )
}

@Composable
fun KeyValueRow(label: String, value: String, valueColor: Color = PrColors.Text) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PrColors.Muted, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RowScope.StatTile(value: String, label: String) {
    Column(
        Modifier
            .weight(1f)
            .background(PrColors.Tile, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
        Text(label, fontSize = 11.sp, color = PrColors.Muted)
    }
}

/** Câmp de alegere dintr-o listă (se deschide la atingere). */
@Composable
fun PickerField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = hint?.let { { Text(it, color = PrColors.Faint) } },
            trailingIcon = { Text("▾", color = PrColors.Muted, fontSize = 16.sp) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
        )
        if (enabled) {
            Box(
                Modifier
                    .matchParentSize()
                    .clickable { open = true },
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = PrColors.Text) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
