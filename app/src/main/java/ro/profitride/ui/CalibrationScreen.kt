package ro.profitride.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import ro.profitride.R
import ro.profitride.core.IdleDetector
import ro.profitride.core.Missing
import ro.profitride.core.ParseResult
import ro.profitride.core.Platform
import ro.profitride.service.ProfitAccessibilityService
import ro.profitride.service.Snapshot
import ro.profitride.util.Format

/**
 * Bolt și Uber nu au API public și își pot schimba ecranele. Aici șoferul vede exact ce text a citit
 * serviciul din fiecare aplicație și ce a înțeles parserul, și poate alege textul care arată că
 * ecranul „se caută curse”. Citirea rămâne doar în memorie; nimic nu pleacă din telefon.
 */
@Composable
fun CalibrationScreen(vm: MainViewModel, onBack: () -> Unit) {
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val serviceEnabled by vm.serviceEnabled.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    val snapshots = remember(tick) { ProfitAccessibilityService.snapshots }
    val offerSnapshots = remember(tick) { ProfitAccessibilityService.offerSnapshots }

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
            Text(stringResource(R.string.cal_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
        }

        PrCard {
            Text(stringResource(R.string.cal_steps), color = PrColors.Text, fontSize = 14.sp, lineHeight = 21.sp)
            if (!serviceEnabled) {
                Hint(stringResource(R.string.cal_needs_service))
            }
            SecondaryButton(stringResource(R.string.cal_refresh), onClick = { tick += 1 })
        }

        Platform.entries.forEach { platform ->
            val markers = overlay.markersFor(platform)
            PlatformCard(
                platform = platform,
                snapshot = snapshots[platform],
                offerSnapshot = offerSnapshots[platform],
                markers = markers,
                onAddMarker = { text ->
                    vm.updateOverlay { s ->
                        val next = s.markersFor(platform) + text.trim()
                        if (platform == Platform.BOLT) s.copy(idleMarkersBolt = next) else s.copy(idleMarkersUber = next)
                    }
                },
                onRemoveMarker = { text ->
                    vm.updateOverlay { s ->
                        val next = s.markersFor(platform) - text
                        if (platform == Platform.BOLT) s.copy(idleMarkersBolt = next) else s.copy(idleMarkersUber = next)
                    }
                },
            )
        }
    }
}

@Composable
fun resultLabel(result: ParseResult): String {
    val missingDistance = stringResource(R.string.cal_missing_distance)
    val missingTime = stringResource(R.string.cal_missing_time)
    return when (result) {
        is ParseResult.Complete -> {
            val o = result.offer
            stringResource(
                R.string.cal_result_complete,
                Format.number(o.price, 2),
                Format.number(o.pickupKm, 1), Format.number(o.pickupMin, 0),
                Format.number(o.tripKm, 1), Format.number(o.tripMin, 0),
            )
        }
        is ParseResult.Incomplete -> stringResource(
            R.string.cal_result_incomplete,
            result.missing.joinToString(", ") { if (it == Missing.DISTANCE) missingDistance else missingTime },
        )
        ParseResult.Unsure -> stringResource(R.string.cal_result_unsure)
        ParseResult.NoOffer -> stringResource(R.string.cal_result_none)
    }
}

@Composable
private fun PlatformCard(
    platform: Platform,
    snapshot: Snapshot?,
    offerSnapshot: Snapshot?,
    markers: Set<String>,
    onAddMarker: (String) -> Unit,
    onRemoveMarker: (String) -> Unit,
) {
    val context = LocalContext.current
    val name = if (platform == Platform.BOLT) "Bolt Driver" else "Uber Driver"

    PrCard {
        Text(name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
        Hint(platform.packageName)

        if (offerSnapshot != null) {
            Label(stringResource(R.string.cal_last_offer))
            val offerAge = ((System.currentTimeMillis() - offerSnapshot.atMillis) / 1000).coerceAtLeast(0)
            Hint(stringResource(R.string.cal_age, offerAge.toString()))
            val offerOk = offerSnapshot.result is ParseResult.Complete
            StatusPill(resultLabel(offerSnapshot.result), if (offerOk) PrColors.Accent else PrColors.Amber)
            offerSnapshot.texts.take(40).forEach { t ->
                Text(
                    t.text,
                    color = PrColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrColors.Surface2, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            SecondaryButton(
                stringResource(R.string.cal_copy),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ProfitRide", offerSnapshot.texts.joinToString("\n") { it.text }))
                },
            )
        }

        if (snapshot == null) {
            Text(stringResource(R.string.cal_nothing), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
            return@PrCard
        }

        val ageSec = ((System.currentTimeMillis() - snapshot.atMillis) / 1000).coerceAtLeast(0)
        Hint(stringResource(R.string.cal_age, ageSec.toString()))
        if (snapshot.visual) Hint(stringResource(R.string.cal_source_visual))

        val result = snapshot.result
        val resultText = resultLabel(result)
        val ok = result is ParseResult.Complete
        StatusPill(resultText, if (ok) PrColors.Accent else PrColors.Amber)

        val idle = IdleDetector.isIdle(snapshot.texts.map { it.text }, markers)
        StatusPill(
            stringResource(if (idle) R.string.cal_idle_yes else R.string.cal_idle_no),
            if (idle) PrColors.Accent else PrColors.Faint,
        )

        if (markers.isNotEmpty()) {
            Label(stringResource(R.string.cal_markers))
            markers.forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRemoveMarker(m) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("“$m”", color = PrColors.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.cal_remove), color = PrColors.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Hint(stringResource(R.string.cal_markers_default))
        }

        Label(stringResource(R.string.cal_read_texts))
        Hint(stringResource(R.string.cal_tap_to_mark))
        snapshot.texts.take(60).forEach { t ->
            Text(
                t.text,
                color = PrColors.Text,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrColors.Surface2, RoundedCornerShape(10.dp))
                    .clickable { onAddMarker(t.text) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        SecondaryButton(
            stringResource(R.string.cal_copy),
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ProfitRide", snapshot.texts.joinToString("\n") { it.text }))
            },
        )
    }
}
