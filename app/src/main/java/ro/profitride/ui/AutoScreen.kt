package ro.profitride.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import ro.profitride.ProfitRideApp
import ro.profitride.R
import ro.profitride.core.AutoData
import ro.profitride.core.AutoLogic
import ro.profitride.core.CarDoc
import ro.profitride.core.DateInput
import ro.profitride.core.DocType
import ro.profitride.core.DueState
import ro.profitride.core.Expense
import ro.profitride.core.ExpenseType
import ro.profitride.core.MaintKind
import ro.profitride.core.Maintenance
import ro.profitride.overlay.OverlayText
import ro.profitride.service.AutoReminders
import ro.profitride.util.AutoNames
import ro.profitride.util.Format

private const val HOME = "home"
private const val DOC_PICK = "docpick"
private const val MAINT = "maint"
private const val EXP = "exp"
private const val CAR = "car"

private fun backTarget(page: String): String = when {
    page.startsWith("m:") -> MAINT
    page == "expnew" || page.startsWith("e:") -> EXP
    else -> HOME
}

@Composable
fun AutoScreen(vm: MainViewModel) {
    val store = remember { ProfitRideApp.instance.autoStore }
    val data by store.state.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(HOME) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        AutoReminders.check(context)
    }
    val onSavedSomething = {
        AutoReminders.schedule(context)
        if (Build.VERSION.SDK_INT >= 33 && !AutoReminders.notificationsAllowed(context)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AutoReminders.check(context)
        }
    }

    BackHandler(enabled = data.tutorialSeen && page != HOME) { page = backTarget(page) }

    if (!data.tutorialSeen) {
        AutoTutorial(onDone = { store.update { it.copy(tutorialSeen = true) } })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val back = { page = backTarget(page) }
        when {
            page == HOME -> AutoHome(data, profile.makeName, profile.modelName, profile.year, { page = it })
            page == DOC_PICK -> DocPicker(data, back) { page = "d:${it.name}" }
            page.startsWith("d:") -> {
                val type = DocType.valueOf(page.removePrefix("d:"))
                DocForm(type, data, store, { page = HOME }, onSavedSomething, vm::updateProfile)
            }
            page == MAINT -> MaintList(data, back) { page = "m:${it.name}" }
            page.startsWith("m:") -> MaintForm(MaintKind.valueOf(page.removePrefix("m:")), data, store) { page = MAINT }
            page == EXP -> ExpenseList(data, profile.let { OverlayText.currency(it) }, back) { page = it }
            page == "expnew" -> ExpenseForm(null, data, store, onBack = { page = EXP })
            page.startsWith("e:") -> ExpenseForm(page.removePrefix("e:").toLongOrNull(), data, store, onBack = { page = EXP })
            page == CAR -> CarForm(data, store, profile.makeName, profile.modelName, profile.year, back)
            else -> {
                page = HOME
            }
        }
    }
}

// ------------------------------------------------------------------ tutorial

@Composable
private fun AutoTutorial(onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IconTile(Icons.Filled.Build, PrColors.Accent, 84)
        ScreenTitle(stringResource(R.string.tut_title), stringResource(R.string.tut_sub))
        TutorialRow(Icons.Filled.DateRange, R.string.tut_1_title, R.string.tut_1_body)
        TutorialRow(Icons.Filled.Notifications, R.string.tut_2_title, R.string.tut_2_body)
        TutorialRow(Icons.Filled.Build, R.string.tut_3_title, R.string.tut_3_body)
        TipCard(stringResource(R.string.tut_tip))
        PrimaryButton(stringResource(R.string.tut_go), onClick = onDone)
        Hint(stringResource(R.string.tut_again))
    }
}

@Composable
private fun TutorialRow(icon: ImageVector, title: Int, body: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        IconTile(icon, PrColors.Accent, 40)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(title), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
            Text(stringResource(body), fontSize = 13.sp, color = PrColors.Muted, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tone: Color, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .background(tone.copy(alpha = 0.14f), RoundedCornerShape((size / 3).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size((size / 2).dp))
    }
}

// ------------------------------------------------------------------ ecranul principal

@Composable
private fun stateColor(state: DueState): Color = when (state) {
    DueState.EXPIRED -> PrColors.Red
    DueState.SOON -> PrColors.Amber
    DueState.OK -> PrColors.Accent
}

@Composable
private fun AutoHome(data: AutoData, make: String, model: String, year: Int, go: (String) -> Unit) {
    val today = LocalDate.now()
    val todayDay = today.toEpochDay()
    ScreenTitle(stringResource(R.string.auto_title))

    val carName = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
        .let { if (it.isBlank()) stringResource(R.string.auto_car_default) else if (year > 0) "$it · $year" else it }
    val carSub = listOfNotNull(
        data.plate.takeIf { it.isNotBlank() },
        data.odometer.takeIf { it > 0 }?.let { stringResource(R.string.auto_odometer, AutoLogic.groupThousands(it)) },
    ).joinToString(" · ")
    PrCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(Icons.Filled.Home, PrColors.Accent, 40)
            Column(Modifier.weight(1f)) {
                Text(carName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                if (carSub.isNotBlank()) Text(carSub, fontSize = 12.sp, color = PrColors.Muted)
            }
            Text(
                stringResource(R.string.auto_car_change),
                color = PrColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clickable { go(CAR) }
                    .padding(8.dp),
            )
        }
    }

    val urgent = data.docs.minByOrNull { it.expiryDay }
    if (urgent != null) {
        val left = AutoLogic.daysLeft(urgent.expiryDay, todayDay)
        if (AutoLogic.docState(left) != DueState.OK) {
            val tone = stateColor(AutoLogic.docState(left))
            val name = stringResource(AutoNames.doc(urgent.type))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(tone.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .clickable { go("d:${urgent.type.name}") }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = tone, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        when {
                            left < 0 -> stringResource(R.string.auto_alert_expired, name)
                            left == 0L -> stringResource(R.string.auto_alert_today, name)
                            else -> stringResource(R.string.auto_alert_days, name, left.toInt())
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text,
                    )
                    Text(
                        DateInput.display(LocalDate.ofEpochDay(urgent.expiryDay)) + " · " + stringResource(R.string.auto_alert_tap),
                        fontSize = 12.sp, color = PrColors.Muted,
                    )
                }
            }
        }
    }

    PrCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.auto_docs))
            LinkText(stringResource(R.string.auto_add)) { go(DOC_PICK) }
        }
        if (data.docs.isEmpty()) {
            Text(stringResource(R.string.auto_no_docs), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        data.docs.sortedBy { it.expiryDay }.forEach { d ->
            val left = AutoLogic.daysLeft(d.expiryDay, todayDay)
            val state = AutoLogic.docState(left)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { go("d:${d.type.name}") }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(if (state == DueState.OK) Icons.Filled.Check else Icons.Filled.Warning, stateColor(state), 40)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(AutoNames.doc(d.type)), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                    Text(
                        stringResource(R.string.auto_expires, DateInput.display(LocalDate.ofEpochDay(d.expiryDay))),
                        fontSize = 12.sp, color = PrColors.Muted,
                    )
                }
                StatusPill(
                    when (state) {
                        DueState.EXPIRED -> stringResource(R.string.auto_expired)
                        DueState.SOON -> stringResource(R.string.auto_days_left, left.toInt())
                        DueState.OK -> stringResource(R.string.auto_valid)
                    },
                    stateColor(state),
                )
            }
        }
    }

    PrCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.auto_maint))
            LinkText(stringResource(R.string.auto_see_all)) { go(MAINT) }
        }
        val next = data.maintenance.minByOrNull { AutoLogic.kmLeft(it, data.odometer) }
        if (next != null && data.odometer > 0) {
            MaintBar(next, data.odometer)
        } else {
            Text(stringResource(R.string.auto_need_odometer), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }

    PrCard(modifier = Modifier.clickable { go(EXP) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.auto_expenses))
            LinkText(stringResource(R.string.auto_see_all)) { go(EXP) }
        }
        Text(
            stringResource(R.string.auto_expenses_month, Format.number(AutoLogic.monthTotal(data.expenses, today), 0)),
            color = PrColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
    Hint(stringResource(R.string.auto_privacy))
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = PrColors.Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}

@Composable
private fun MaintBar(m: Maintenance, odometer: Long) {
    val left = AutoLogic.kmLeft(m, odometer)
    val tone = stateColor(AutoLogic.maintState(left))
    val frac = AutoLogic.usedFraction(m, odometer).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(AutoNames.maint(m.kind)), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
            Text(
                if (left >= 0) stringResource(R.string.auto_km_left, AutoLogic.groupThousands(left))
                else stringResource(R.string.auto_km_over, AutoLogic.groupThousands(-left)),
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = tone,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(PrColors.Outline, RoundedCornerShape(3.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(6.dp)
                    .background(tone, RoundedCornerShape(3.dp)),
            )
        }
    }
}

// ------------------------------------------------------------------ documente

@Composable
private fun DocPicker(data: AutoData, onBack: () -> Unit, onPick: (DocType) -> Unit) {
    BackHeader(stringResource(R.string.auto_pick_doc), stringResource(R.string.auto_pick_doc_sub), onBack)
    PrCard {
        DocType.entries.forEach { type ->
            val added = data.docs.any { it.type == type }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(type) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(if (added) Icons.Filled.Check else Icons.Filled.Add, if (added) PrColors.Accent else PrColors.Muted, 40)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(AutoNames.doc(type)), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                    Text(stringResource(AutoNames.docSub(type)), fontSize = 12.sp, color = PrColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun DocForm(
    type: DocType,
    data: AutoData,
    store: ro.profitride.data.AutoStore,
    onDone: () -> Unit,
    onSaved: () -> Unit,
    updateProfile: ((ro.profitride.core.DriverProfile) -> ro.profitride.core.DriverProfile) -> Unit,
) {
    val existing = data.docs.firstOrNull { it.type == type }
    var dateText by rememberSaveable(type) {
        mutableStateOf(existing?.let { DateInput.display(LocalDate.ofEpochDay(it.expiryDay)) }.orEmpty())
    }
    var cost by rememberSaveable(type) { mutableStateOf(existing?.cost ?: 0.0) }
    var notify by remember(type) { mutableStateOf((existing?.notifyDays ?: AutoLogic.defaultNotifyDays).toList().sorted()) }
    val valid = DateInput.validate(dateText) as? DateInput.Result.Valid

    BackHeader(stringResource(AutoNames.doc(type)), stringResource(AutoNames.docSub(type)), onDone)
    TipCard(stringResource(AutoNames.docTip(type)))
    DateField(stringResource(R.string.auto_date_label), dateText, { dateText = it }, stringResource(R.string.auto_date_help))
    NumberField(
        label = stringResource(R.string.auto_cost_label),
        value = cost,
        onValueChange = { cost = it },
        suffix = "lei",
        help = stringResource(if (type.syncsToProfile) R.string.auto_cost_help_sync else R.string.auto_cost_help),
    )
    Label(stringResource(R.string.auto_notify))
    MultiChips(
        options = listOf(30, 10, 3, 0),
        selected = notify.toSet(),
        label = {
            stringResource(
                when (it) {
                    30 -> R.string.auto_notify_30
                    10 -> R.string.auto_notify_10
                    3 -> R.string.auto_notify_3
                    else -> R.string.auto_notify_0
                },
            )
        },
        onToggle = { d -> notify = (if (d in notify) notify - d else notify + d).sorted() },
    )
    if (valid == null) Hint(stringResource(R.string.auto_need_date))
    PrimaryButton(
        stringResource(R.string.auto_save_doc, stringResource(AutoNames.doc(type))),
        enabled = valid != null,
        onClick = {
            val date = valid ?: return@PrimaryButton
            val doc = CarDoc(type, date.date.toEpochDay(), cost, notify.toSet())
            store.update { d -> d.copy(docs = d.docs.filter { it.type != type } + doc) }
            if (cost > 0) {
                when (type) {
                    DocType.RCA -> updateProfile { it.copy(rcaYearly = cost) }
                    DocType.CASCO -> updateProfile { it.copy(cascoYearly = cost) }
                    else -> Unit
                }
            }
            onSaved()
            onDone()
        },
    )
    if (existing != null) {
        SecondaryButton(stringResource(R.string.auto_delete), onClick = {
            store.update { d -> d.copy(docs = d.docs.filter { it.type != type }) }
            onDone()
        })
    }
}

// ------------------------------------------------------------------ întreținere

@Composable
private fun MaintList(data: AutoData, onBack: () -> Unit, onPick: (MaintKind) -> Unit) {
    BackHeader(stringResource(R.string.auto_maint), stringResource(R.string.auto_maint_sub), onBack)
    PrCard {
        MaintKind.entries.forEachIndexed { i, kind ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(PrColors.Outline))
            val item = data.maintenance.firstOrNull { it.kind == kind }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(kind) }
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (item != null && data.odometer > 0) {
                    MaintBar(item, data.odometer)
                    Hint(stringResource(R.string.auto_maint_last, AutoLogic.groupThousands(item.lastKm), AutoLogic.groupThousands(item.intervalKm)))
                } else if (item != null) {
                    Text(stringResource(AutoNames.maint(kind)), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                    Hint(stringResource(R.string.auto_maint_last, AutoLogic.groupThousands(item.lastKm), AutoLogic.groupThousands(item.intervalKm)))
                    Hint(stringResource(R.string.auto_need_odometer))
                } else {
                    Text(stringResource(AutoNames.maint(kind)), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                    Hint(stringResource(R.string.auto_not_set))
                }
            }
        }
    }
    TipCard(stringResource(R.string.auto_maint_tip))
}

@Composable
private fun MaintForm(kind: MaintKind, data: AutoData, store: ro.profitride.data.AutoStore, onDone: () -> Unit) {
    val existing = data.maintenance.firstOrNull { it.kind == kind }
    var note by rememberSaveable(kind) { mutableStateOf(existing?.note.orEmpty()) }
    var km by rememberSaveable(kind) { mutableStateOf(existing?.lastKm ?: data.odometer) }
    var interval by rememberSaveable(kind) { mutableStateOf(existing?.intervalKm ?: kind.defaultIntervalKm) }

    BackHeader(stringResource(AutoNames.maint(kind)), stringResource(R.string.maint_form_sub), onDone)
    TipCard(stringResource(R.string.maint_tip))
    TextInput(
        label = stringResource(R.string.maint_note),
        value = note,
        onValueChange = { note = it },
        help = stringResource(R.string.maint_note_help),
        capitalization = KeyboardCapitalization.Sentences,
    )
    KmField(stringResource(R.string.maint_km_at), km, { km = it }, stringResource(R.string.maint_km_at_help))
    KmField(stringResource(R.string.maint_interval), interval, { interval = it }, stringResource(R.string.maint_interval_help))
    PrimaryButton(
        stringResource(R.string.maint_save),
        enabled = km > 0 && interval > 0,
        onClick = {
            val item = Maintenance(kind, km, interval, note.trim(), LocalDate.now().toEpochDay())
            store.update { d ->
                d.copy(
                    maintenance = d.maintenance.filter { it.kind != kind } + item,
                    odometer = maxOf(d.odometer, km),
                )
            }
            onDone()
        },
    )
    if (existing != null) {
        SecondaryButton(stringResource(R.string.auto_delete), onClick = {
            store.update { d -> d.copy(maintenance = d.maintenance.filter { it.kind != kind }) }
            onDone()
        })
    }
}

// ------------------------------------------------------------------ mașina (număr, kilometraj)

@Composable
private fun CarForm(data: AutoData, store: ro.profitride.data.AutoStore, make: String, model: String, year: Int, onDone: () -> Unit) {
    var plate by rememberSaveable { mutableStateOf(data.plate) }
    var km by rememberSaveable { mutableStateOf(data.odometer) }
    BackHeader(stringResource(R.string.car_form_title), stringResource(R.string.car_form_sub), onDone)
    Hint(stringResource(R.string.car_make_from_profile))
    TextInput(
        label = stringResource(R.string.car_plate),
        value = plate,
        onValueChange = { plate = it.uppercase() },
        help = stringResource(R.string.car_plate_help),
        capitalization = KeyboardCapitalization.Characters,
    )
    KmField(stringResource(R.string.car_odometer), km, { km = it }, stringResource(R.string.car_odometer_help))
    PrimaryButton(stringResource(R.string.car_save), onClick = {
        store.update { it.copy(plate = plate.trim(), odometer = km) }
        onDone()
    })
}

// ------------------------------------------------------------------ cheltuieli

@Composable
private fun ExpenseList(data: AutoData, cur: String, onBack: () -> Unit, go: (String) -> Unit) {
    val today = LocalDate.now()
    BackHeader(stringResource(R.string.exp_title), stringResource(R.string.exp_sub), onBack)
    PrCard {
        Label(stringResource(R.string.exp_this_month))
        Text(
            Format.number(AutoLogic.monthTotal(data.expenses, today), 0) + " " + cur,
            fontSize = 36.sp, fontWeight = FontWeight.Bold, color = PrColors.Text,
        )
        Hint(stringResource(R.string.exp_note_total))
    }
    PrCard {
        if (data.expenses.isEmpty()) {
            Text(stringResource(R.string.exp_empty), color = PrColors.Muted, fontSize = 14.sp)
        }
        data.expenses.sortedByDescending { it.day }.take(60).forEach { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { go("e:${e.id}") }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(AutoNames.expense(e.type)), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
                    val sub = buildString {
                        append(DateInput.display(LocalDate.ofEpochDay(e.day)))
                        if (e.liters > 0) append(" · ").append(Format.number(e.liters, 1)).append(" l")
                        if (e.note.isNotBlank()) append(" · ").append(e.note)
                    }
                    Text(sub, fontSize = 12.sp, color = PrColors.Muted)
                }
                Text(Format.number(e.amount, 2) + " " + cur, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
            }
        }
    }
    PrimaryButton(stringResource(R.string.exp_add), onClick = { go("expnew") })
}

@Composable
private fun ExpenseForm(id: Long?, data: AutoData, store: ro.profitride.data.AutoStore, onBack: () -> Unit) {
    val existing = id?.let { i -> data.expenses.firstOrNull { it.id == i } }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: ExpenseType.ALIMENTARE) }
    var amount by rememberSaveable { mutableStateOf(existing?.amount ?: 0.0) }
    var liters by rememberSaveable { mutableStateOf(existing?.liters ?: 0.0) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var dateText by rememberSaveable {
        mutableStateOf(DateInput.display(LocalDate.ofEpochDay(existing?.day ?: LocalDate.now().toEpochDay())))
    }
    val valid = DateInput.validate(dateText) as? DateInput.Result.Valid

    BackHeader(stringResource(if (existing == null) R.string.exp_new else R.string.exp_edit), null, onBack)
    Label(stringResource(R.string.exp_type))
    ChipRow(ExpenseType.entries, type, { stringResource(AutoNames.expense(it)) }, { type = it })
    TipCard(stringResource(R.string.exp_tip))
    NumberField(stringResource(R.string.exp_amount), amount, { amount = it }, suffix = "lei", help = stringResource(R.string.exp_amount_help))
    if (type == ExpenseType.ALIMENTARE) {
        NumberField(stringResource(R.string.exp_liters), liters, { liters = it }, suffix = "l", help = stringResource(R.string.exp_liters_help))
    }
    DateField(stringResource(R.string.exp_date), dateText, { dateText = it }, stringResource(R.string.exp_date_help))
    TextInput(
        label = stringResource(R.string.exp_note),
        value = note,
        onValueChange = { note = it },
        help = stringResource(R.string.exp_note_help),
        capitalization = KeyboardCapitalization.Sentences,
    )
    PrimaryButton(
        stringResource(R.string.exp_save),
        enabled = amount > 0 && valid != null,
        onClick = {
            val date = valid ?: return@PrimaryButton
            val e = Expense(
                id = existing?.id ?: System.currentTimeMillis(),
                type = type,
                amount = amount,
                liters = if (type == ExpenseType.ALIMENTARE) liters else 0.0,
                day = date.date.toEpochDay(),
                note = note.trim(),
            )
            store.update { d -> d.copy(expenses = d.expenses.filter { it.id != e.id } + e) }
            onBack()
        },
    )
    if (existing != null) {
        SecondaryButton(stringResource(R.string.auto_delete), onClick = {
            store.update { d -> d.copy(expenses = d.expenses.filter { it.id != existing.id }) }
            onBack()
        })
    }
}
