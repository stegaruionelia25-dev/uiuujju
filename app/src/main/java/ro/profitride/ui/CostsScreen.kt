package ro.profitride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import ro.profitride.R
import ro.profitride.core.CostMath
import ro.profitride.overlay.OverlayText

private val SECTIONS: List<Pair<Int, List<CostId>>> = listOf(
    R.string.ct_sec_app to listOf(CostId.COUNTRY),
    R.string.ct_sec_machine to listOf(CostId.CAR, CostId.FUEL, CostId.CONSUMPTION, CostId.PRICE, CostId.KM),
    R.string.ct_sec_insurance to listOf(CostId.RCA, CostId.CASCO, CostId.ITP, CostId.VIGNETTE, CostId.TAXES, CostId.OTHER),
    R.string.ct_sec_wear to listOf(CostId.WEAR),
    R.string.ct_sec_fixed to listOf(CostId.RENT, CostId.ACCOUNTING, CostId.SUBS),
    R.string.ct_sec_platform to listOf(CostId.BOLT, CostId.UBER, CostId.FLEET_PCT, CostId.FLEET_FIX, CostId.TAX_RES),
    R.string.ct_sec_thresholds to listOf(CostId.BASIS, CostId.THRESHOLD, CostId.DAILY),
    R.string.ct_sec_help to listOf(CostId.TOUR),
)

/** Rândurile pe care le luminează turul, în ordine. Lungimea trebuie să fie [CostMath.TOUR_STEPS]. */
private val TOUR_TARGETS = listOf(
    CostId.COUNTRY, CostId.FUEL, CostId.CONSUMPTION, CostId.PRICE, CostId.RCA,
    CostId.WEAR, CostId.RENT, CostId.BOLT, CostId.THRESHOLD,
)
private val TOUR_TITLES = intArrayOf(
    R.string.ct_step1_title, R.string.ct_step2_title, R.string.ct_step3_title, R.string.ct_step4_title, R.string.ct_step5_title,
    R.string.ct_step6_title, R.string.ct_step7_title, R.string.ct_step8_title, R.string.ct_step9_title,
)
private val TOUR_BODIES = intArrayOf(
    R.string.ct_step1_body, R.string.ct_step2_body, R.string.ct_step3_body, R.string.ct_step4_body, R.string.ct_step5_body,
    R.string.ct_step6_body, R.string.ct_step7_body, R.string.ct_step8_body, R.string.ct_step9_body,
)

/**
 * „Costurile mele”: toate costurile șoferului, ca rânduri. Atingi un rând ca să-l editezi, atingi „i” ca să afli
 * ce înseamnă. La prima intrare apare invitația la tutorial (9 pași).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostsScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val cur = OverlayText.currency(profile)

    // -1 = nimic deschis; altfel poziția din CostId
    var editing by rememberSaveable { mutableIntStateOf(-1) }
    var infoFor by rememberSaveable { mutableIntStateOf(-1) }
    // 0 = fără tur; 1..9 = pasul curent
    var tourStep by rememberSaveable { mutableIntStateOf(0) }

    val bounds: SnapshotStateMap<CostId, Rect> = remember { mutableStateMapOf() }
    val scroll = rememberScrollState()
    var viewportTop by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    fun finishTour() {
        tourStep = 0
        vm.updateOverlay { it.copy(costTourDone = true) }
    }

    // Aduce rândul pasului curent în partea de sus a ecranului, deasupra cardului cu explicația.
    LaunchedEffect(tourStep) {
        if (tourStep in 1..CostMath.TOUR_STEPS) {
            val id = TOUR_TARGETS[tourStep - 1]
            val r = snapshotFlow { bounds[id] }.filterNotNull().first()
            val delta = r.top - viewportTop - with(density) { 24.dp.toPx() }
            scroll.animateScrollBy(delta)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { viewportTop = it.positionInRoot().y }
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle(stringResource(R.string.ct_title), stringResource(R.string.ct_sub))
            CostPerKmCard(profile)
            SECTIONS.forEach { (titleRes, ids) ->
                CostSectionCard(
                    title = stringResource(titleRes),
                    ids = ids,
                    profile = profile,
                    bounds = bounds,
                    onEdit = { id -> if (id == CostId.TOUR) tourStep = 1 else editing = id.ordinal },
                    onInfo = { id -> infoFor = id.ordinal },
                )
            }
            Hint(stringResource(R.string.ct_footer))
            if (tourStep > 0) Spacer(Modifier.height(320.dp))
        }

        if (tourStep in 1..CostMath.TOUR_STEPS) {
            TourLayer(
                step = tourStep,
                target = TOUR_TARGETS[tourStep - 1],
                bounds = bounds,
                onBack = { if (tourStep > 1) tourStep -= 1 },
                onNext = { if (tourStep < CostMath.TOUR_STEPS) tourStep += 1 else finishTour() },
                onClose = ::finishTour,
            )
        }
    }

    if (tourStep == 0 && !overlay.costTourDone && editing == -1 && infoFor == -1) {
        TourInvite(
            onStart = { tourStep = 1 },
            onLater = ::finishTour,
        )
    }
    if (tourStep == 0) {
        CostId.entries.getOrNull(editing)?.let { id ->
            CostEditSheet(id, profile, vm::updateProfile, onClose = { editing = -1 })
        }
        CostId.entries.getOrNull(infoFor)?.let { id ->
            CostInfoSheet(id, cur, onClose = { infoFor = -1 })
        }
    }
}

@Composable
private fun CostSectionCard(
    title: String,
    ids: List<CostId>,
    profile: ro.profitride.core.DriverProfile,
    bounds: SnapshotStateMap<CostId, Rect>,
    onEdit: (CostId) -> Unit,
    onInfo: (CostId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = PrColors.Muted,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = PrColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrColors.Outline),
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ids.forEachIndexed { i, id ->
                    val (value, missing) = costValue(id, profile)
                    CostRow(
                        id = id,
                        value = value,
                        missing = missing,
                        bounds = bounds,
                        onEdit = { onEdit(id) },
                        onInfo = { onInfo(id) },
                    )
                    if (i < ids.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(PrColors.Outline),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostRow(
    id: CostId,
    value: String,
    missing: Boolean,
    bounds: SnapshotStateMap<CostId, Rect>,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
) {
    val title = stringResource(id.title)
    val cd = stringResource(R.string.ct_info_cd, title)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .onGloballyPositioned { bounds[id] = it.boundsInRoot() }
            .clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            color = PrColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            value,
            modifier = Modifier.padding(start = 8.dp),
            color = if (missing) PrColors.Amber else PrColors.Hi,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
        )
        Text("›", modifier = Modifier.padding(start = 6.dp), color = PrColors.Faint, fontSize = 16.sp)
        Box(
            Modifier
                .size(44.dp)
                .semantics { contentDescription = cd }
                .clickable(onClick = onInfo),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .border(2.5.dp, PrColors.Hi, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("i", color = PrColors.Hi, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** Foaia de la prima intrare: invită la tutorial. „Mai târziu” îl oprește; îl reiei din Setări sau de la Ajutor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TourInvite(onStart: () -> Unit, onLater: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onLater,
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
                Box(
                    Modifier
                        .size(64.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF4C7BFF), Color(0xFF2F5BFF))), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(38.dp)) {
                        val s = size.width / 32f
                        val path = Path().apply {
                            moveTo(8f * s, 25f * s)
                            lineTo(8f * s, 8f * s)
                            lineTo(24f * s, 25f * s)
                            lineTo(24f * s, 7f * s)
                        }
                        drawPath(path, Color.White, style = Stroke(width = 6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                Text(stringResource(R.string.ct_invite_title), fontSize = 26.sp, fontWeight = FontWeight.Black, color = PrColors.Text, lineHeight = 29.sp)
                Text(stringResource(R.string.ct_invite_body), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrColors.Muted, lineHeight = 22.sp)
                Text(stringResource(R.string.ct_invite_hint), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrColors.Faint, lineHeight = 19.sp)
                PrimaryButton(stringResource(R.string.ct_tour_start), onStart)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onLater),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.ct_tour_later), color = PrColors.Faint, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/** Ecranul se întunecă, rândul pasului rămâne luminat cu contur, iar jos stă cardul cu explicația. */
@Composable
private fun TourLayer(
    step: Int,
    target: CostId,
    bounds: Map<CostId, Rect>,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val ring = PrColors.Hi
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val hole = bounds[target]?.translate(-origin)?.inflate(8.dp.toPx())
            val radius = CornerRadius(22.dp.toPx())
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                if (hole != null) addRoundRect(RoundRect(hole, radius))
            }
            drawPath(path, Color(0xC7060A2E))
            if (hole != null) {
                drawRoundRect(ring, hole.topLeft, hole.size, radius, style = Stroke(3.dp.toPx()))
            }
        }
        TourCard(step, onBack, onNext, onClose, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TourCard(step: Int, onBack: () -> Unit, onNext: () -> Unit, onClose: () -> Unit, modifier: Modifier) {
    SheetTheme {
        Surface(
            modifier = modifier
                .padding(14.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = SheetContainer,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..CostMath.TOUR_STEPS) {
                            Box(
                                Modifier
                                    .height(7.dp)
                                    .width(if (i == step) 18.dp else 7.dp)
                                    .background(if (i == step) PrColors.Brand else PrColors.Outline, CircleShape),
                            )
                        }
                    }
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable(onClick = onClose)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(stringResource(R.string.ct_tour_close), color = PrColors.Faint, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    stringResource(R.string.ct_tour_step, step, CostMath.TOUR_STEPS),
                    color = PrColors.Brand,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(stringResource(TOUR_TITLES[step - 1]), color = PrColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Black, lineHeight = 26.sp)
                Text(stringResource(TOUR_BODIES[step - 1]), color = PrColors.Muted, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step > 1) {
                        SecondaryButton(stringResource(R.string.ct_tour_back), onClick = onBack, modifier = Modifier.weight(0.7f))
                    }
                    PrimaryButton(
                        stringResource(if (step < CostMath.TOUR_STEPS) R.string.ct_tour_next else R.string.ct_tour_finish),
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
