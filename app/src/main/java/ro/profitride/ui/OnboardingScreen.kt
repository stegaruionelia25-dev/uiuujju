package ro.profitride.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.profitride.R

private const val STEPS = 6 // 0 = bun venit, 1..5 = configurare

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val serviceEnabled by vm.serviceEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = step > 0) { step -= 1 }

    val canContinue = when (step) {
        3 -> profile.consumption > 0 && profile.energyPrice > 0 && profile.yearlyKm > 0
        4 -> profile.goodThreshold > 0
        else -> true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PrColors.Bg)
            .safeDrawingPadding(),
    ) {
        if (step > 0) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(PrColors.Surface, RoundedCornerShape(14.dp))
                            .clickable { step -= 1 }
                            .semantics { contentDescription = context.getString(R.string.back) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PrColors.Text)
                    }
                    Text(
                        stringResource(R.string.step_of, step, STEPS - 1),
                        color = PrColors.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1 until STEPS) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (i <= step) PrColors.Accent else PrColors.Outline, RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> {
                    ScreenTitle(stringResource(R.string.ob_country_title), stringResource(R.string.ob_country_sub))
                    CountrySection(profile, vm::updateProfile)
                    AppsInfo()
                }
                2 -> {
                    ScreenTitle(stringResource(R.string.ob_car_title), stringResource(R.string.ob_car_sub))
                    CarSection(profile, vm::updateProfile)
                }
                3 -> {
                    ScreenTitle(stringResource(R.string.ob_cost_title), stringResource(R.string.ob_cost_sub))
                    CostSection(profile, vm::updateProfile)
                    CostPerKmCard(profile)
                    Hint(stringResource(R.string.ob_cost_estimate_hint))
                }
                4 -> {
                    ScreenTitle(stringResource(R.string.ob_earn_title), stringResource(R.string.ob_earn_sub))
                    EarningsSection(profile, vm::updateProfile)
                }
                else -> PermissionStep(serviceEnabled)
            }
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            PrimaryButton(
                text = when (step) {
                    0 -> stringResource(R.string.ob_start)
                    STEPS - 1 -> stringResource(R.string.ob_finish)
                    else -> stringResource(R.string.next)
                },
                enabled = canContinue,
                onClick = {
                    if (step == STEPS - 1) {
                        vm.updateProfile { it.copy(onboardingDone = true) }
                    } else {
                        step += 1
                    }
                },
            )
            if (!canContinue) {
                Text(
                    stringResource(R.string.ob_fill_required),
                    color = PrColors.Faint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}


@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(PrColors.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(PrColors.Bg, CircleShape),
                )
            }
            Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.ob_welcome_title),
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = PrColors.Text,
            )
            Text(stringResource(R.string.ob_welcome_sub), fontSize = 15.sp, lineHeight = 22.sp, color = PrColors.Muted)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label(stringResource(R.string.ob_example_label))
            SampleBubble()
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TutorialLine(1, stringResource(R.string.ob_tut1_title), stringResource(R.string.ob_tut1_sub))
            TutorialLine(2, stringResource(R.string.ob_tut2_title), stringResource(R.string.ob_tut2_sub))
            TutorialLine(3, stringResource(R.string.ob_tut3_title), stringResource(R.string.ob_tut3_sub))
        }
    }
}

@Composable
private fun TutorialLine(number: Int, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .background(PrColors.Accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), color = PrColors.Accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
            Text(subtitle, fontSize = 13.sp, color = PrColors.Muted)
        }
    }
}

/** Machetă statică a bulei, doar pentru tutorial. Cifrele sunt un exemplu, nu date reale. */
@Composable
private fun SampleBubble() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PrColors.Surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .background(PrColors.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(PrColors.Bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("84", color = PrColors.Accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("+16,9 lei", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrColors.Text)
            Text("net · 11,5 km · 24 min", fontSize = 12.sp, color = PrColors.Muted)
        }
        StatusPill(stringResource(R.string.rating_good), PrColors.Accent)
    }
}

@Composable
private fun AppsInfo() {
    PrCard {
        Label(stringResource(R.string.ob_apps_label))
        Text(stringResource(R.string.ob_apps_body), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        Hint(stringResource(R.string.ob_apps_hint))
    }
}

@Composable
private fun PermissionStep(serviceEnabled: Boolean) {
    val context = LocalContext.current
    ScreenTitle(stringResource(R.string.ob_perm_title), stringResource(R.string.ob_perm_sub))
    PrCard(border = if (serviceEnabled) PrColors.Accent.copy(alpha = 0.5f) else PrColors.Amber.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.perm_accessibility), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
            if (serviceEnabled) {
                StatusPill(stringResource(R.string.perm_active), PrColors.Accent)
            } else {
                StatusPill(stringResource(R.string.perm_inactive), PrColors.Amber)
            }
        }
        Text(stringResource(R.string.perm_accessibility_body), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        if (!serviceEnabled) {
            PrimaryButton(stringResource(R.string.perm_open_accessibility), onClick = { AccessibilityStatus.openAccessibilitySettings(context) })
        }
    }
    PrCard {
        Label(stringResource(R.string.perm_how_title))
        Text(stringResource(R.string.perm_how_steps), color = PrColors.Text, fontSize = 14.sp, lineHeight = 21.sp)
        Hint(stringResource(R.string.perm_restricted_hint))
        SecondaryButton(stringResource(R.string.perm_open_app_details), onClick = { AccessibilityStatus.openAppDetails(context) })
    }
    PrCard {
        Label(stringResource(R.string.perm_privacy_title))
        Text(stringResource(R.string.perm_privacy_body), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
    }
    Hint(stringResource(R.string.ob_perm_skip_hint))
}
