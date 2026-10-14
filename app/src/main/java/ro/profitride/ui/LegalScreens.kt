package ro.profitride.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.profitride.BuildConfig
import ro.profitride.ProfitRideApp
import ro.profitride.R
import ro.profitride.data.DataRights
import java.text.DateFormat
import java.util.Date

private val TERMS = listOf(
    R.string.terms_1_h to R.string.terms_1_b,
    R.string.terms_2_h to R.string.terms_2_b,
    R.string.terms_3_h to R.string.terms_3_b,
    R.string.terms_4_h to R.string.terms_4_b,
    R.string.terms_5_h to R.string.terms_5_b,
    R.string.terms_6_h to R.string.terms_6_b,
    R.string.terms_7_h to R.string.terms_7_b,
    R.string.terms_8_h to R.string.terms_8_b,
    R.string.terms_9_h to R.string.terms_9_b,
    R.string.terms_10_h to R.string.terms_10_b,
    R.string.terms_11_h to R.string.terms_11_b,
    R.string.terms_12_h to R.string.terms_12_b,
    R.string.terms_13_h to R.string.terms_13_b,
    R.string.terms_14_h to R.string.terms_14_b,
)

private val PRIVACY = listOf(
    R.string.privacy_1_h to R.string.privacy_1_b,
    R.string.privacy_2_h to R.string.privacy_2_b,
    R.string.privacy_3_h to R.string.privacy_3_b,
    R.string.privacy_4_h to R.string.privacy_4_b,
    R.string.privacy_5_h to R.string.privacy_5_b,
    R.string.privacy_6_h to R.string.privacy_6_b,
    R.string.privacy_7_h to R.string.privacy_7_b,
    R.string.privacy_8_h to R.string.privacy_8_b,
    R.string.privacy_9_h to R.string.privacy_9_b,
    R.string.privacy_10_h to R.string.privacy_10_b,
    R.string.privacy_11_h to R.string.privacy_11_b,
    R.string.privacy_12_h to R.string.privacy_12_b,
)

private enum class LegalPage { HUB, TERMS, PRIVACY, A11Y, DATA, ABOUT }

// ---------------------------------------------------------------- acordul de la prima deschidere

/** Primul ecran: acordul. După „Accept” aplicația continuă direct cu setup-ul obișnuit. */
@Composable
fun ConsentGate(onExit: () -> Unit) {
    var declined by rememberSaveable { mutableStateOf(false) }
    var doc by rememberSaveable { mutableStateOf(0) } // 0 = niciunul, 1 = termeni, 2 = confidențialitate
    when {
        doc != 0 -> {
            BackHandler { doc = 0 }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                LegalDocument(doc == 1, onBack = { doc = 0 })
            }
        }
        declined -> {
            BackHandler { declined = false }
            DeclinedScreen(onBack = { declined = false }, onExit = onExit)
        }
        else -> ConsentScreen(
            onOpenTerms = { doc = 1 },
            onOpenPrivacy = { doc = 2 },
            onAccept = { ProfitRideApp.instance.consentStore.accept() },
            onDecline = { declined = true },
        )
    }
}

@Composable
private fun ConsentScreen(onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    var terms by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    var age by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.consent_title), stringResource(R.string.consent_sub))
        PrCard {
            listOf(R.string.consent_b1, R.string.consent_b2, R.string.consent_b3, R.string.consent_b4).forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 7.dp).size(8.dp).background(PrColors.Accent, RoundedCornerShape(4.dp)))
                    Text(stringResource(it), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
        PrCard {
            ConsentRow(terms, { terms = it }, stringResource(R.string.consent_terms_pre), stringResource(R.string.consent_terms_link), null, onOpenTerms)
            ConsentRow(privacy, { privacy = it }, stringResource(R.string.consent_privacy_pre), stringResource(R.string.consent_privacy_link), null, onOpenPrivacy)
            ConsentRow(age, { age = it }, stringResource(R.string.consent_age), null, stringResource(R.string.consent_age_sub), null)
        }
        Hint(stringResource(R.string.consent_footer))
        PrimaryButton(stringResource(R.string.consent_accept), onClick = onAccept, enabled = terms && privacy && age)
        SecondaryButton(stringResource(R.string.consent_decline), onClick = onDecline)
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    pre: String,
    link: String?,
    sub: String?,
    onLink: (() -> Unit)?,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = PrColors.Accent, uncheckedColor = PrColors.Muted),
        )
        Column(Modifier.padding(top = 12.dp)) {
            if (link != null && onLink != null) {
                val text = buildAnnotatedString {
                    append("$pre ")
                    withStyle(SpanStyle(color = PrColors.Accent, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(link) }
                }
                Text(text, color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.clickable(onClick = onLink))
            } else {
                Text(pre, color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (sub != null) Text(sub, color = PrColors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DeclinedScreen(onBack: () -> Unit, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.declined_title))
        PrCard(border = PrColors.Amber.copy(alpha = 0.5f)) {
            Text(stringResource(R.string.declined_body), color = PrColors.Text, fontSize = 14.sp, lineHeight = 21.sp)
            listOf(R.string.declined_b1, R.string.declined_b2, R.string.declined_b3).forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 7.dp).size(8.dp).background(PrColors.Amber, RoundedCornerShape(4.dp)))
                    Text(stringResource(it), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
        PrimaryButton(stringResource(R.string.declined_back), onClick = onBack)
        SecondaryButton(stringResource(R.string.declined_exit), onClick = onExit)
    }
}

// ---------------------------------------------------------------- Legal și date (din Setări)

@Composable
fun LegalFlow(onClose: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(LegalPage.HUB) }
    BackHandler { if (page == LegalPage.HUB) onClose() else page = LegalPage.HUB }
    val back = { if (page == LegalPage.HUB) onClose() else page = LegalPage.HUB }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (page) {
            LegalPage.HUB -> LegalHub(back) { page = it }
            LegalPage.TERMS -> LegalDocument(true, back)
            LegalPage.PRIVACY -> LegalDocument(false, back)
            LegalPage.A11Y -> A11yPage(back)
            LegalPage.DATA -> DataPage(back)
            LegalPage.ABOUT -> AboutPage(back)
        }
    }
}

@Composable
private fun LegalHub(onBack: () -> Unit, open: (LegalPage) -> Unit) {
    val consent = ProfitRideApp.instance.consentStore
    val at = consent.acceptedAtMillis()
    val v = stringResource(R.string.legal_terms_version)
    BackHeader(stringResource(R.string.legal_title), onBack = onBack)
    if (at > 0) {
        PrCard(border = PrColors.Accent.copy(alpha = 0.4f)) {
            Text(stringResource(R.string.legal_accepted, v), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at)), color = PrColors.Muted, fontSize = 13.sp)
        }
    }
    PrCard {
        LegalRow(stringResource(R.string.legal_row_terms), stringResource(R.string.legal_row_terms_sub, v)) { open(LegalPage.TERMS) }
        LegalRow(stringResource(R.string.legal_row_privacy), stringResource(R.string.legal_row_privacy_sub)) { open(LegalPage.PRIVACY) }
        LegalRow(stringResource(R.string.legal_row_a11y), stringResource(R.string.legal_row_a11y_sub)) { open(LegalPage.A11Y) }
        LegalRow(stringResource(R.string.legal_row_data), stringResource(R.string.legal_row_data_sub)) { open(LegalPage.DATA) }
        LegalRow(stringResource(R.string.legal_row_about), stringResource(R.string.legal_row_about_sub)) { open(LegalPage.ABOUT) }
    }
    TipCard(stringResource(R.string.legal_tip))
}

@Composable
private fun LegalRow(title: String, sub: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = PrColors.Text, fontSize = 15.sp)
        Text(sub, color = PrColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun LegalDocument(terms: Boolean, onBack: () -> Unit) {
    val items = if (terms) TERMS else PRIVACY
    val ctx = LocalContext.current
    val args = arrayOf(
        stringResource(R.string.legal_operator),
        stringResource(R.string.legal_id),
        stringResource(R.string.legal_address),
        stringResource(R.string.legal_email),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackHeader(
            stringResource(if (terms) R.string.legal_row_terms else R.string.legal_row_privacy),
            stringResource(R.string.legal_version_line, stringResource(R.string.legal_terms_version), stringResource(R.string.legal_effective)),
            onBack,
        )
        items.forEachIndexed { i, (h, b) ->
            PrCard {
                Text("${i + 1}. ${stringResource(h)}", fontWeight = FontWeight.ExtraBold, color = PrColors.Text, fontSize = 15.sp)
                Text(ctx.getString(b, *args), color = PrColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun A11yPage(onBack: () -> Unit) {
    BackHeader(stringResource(R.string.a11y_title), onBack = onBack)
    A11yText()
}

@Composable
private fun A11yText() {
    PrCard {
        Text(stringResource(R.string.a11y_intro), color = PrColors.Text, fontSize = 14.sp, lineHeight = 21.sp)
        Label(stringResource(R.string.a11y_reads_h))
        Text("• " + stringResource(R.string.a11y_reads_1), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        Text("• " + stringResource(R.string.a11y_reads_2), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        Label(stringResource(R.string.a11y_never_h))
        Text("• " + stringResource(R.string.a11y_never_1), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        Text("• " + stringResource(R.string.a11y_never_2), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        Text("• " + stringResource(R.string.a11y_never_3), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        Hint(stringResource(R.string.a11y_stop))
    }
}

/**
 * Dezvăluirea vizibilă cerută de Google Play: se arată înainte de a trimite șoferul în setările de accesibilitate.
 * Returnează funcția care deschide dialogul.
 */
@Composable
fun rememberAccessibilityOpener(): () -> Unit {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = PrColors.Surface,
            title = { Text(stringResource(R.string.a11y_title), color = PrColors.Text, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.a11y_intro), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
                    Label(stringResource(R.string.a11y_reads_h))
                    Text("• " + stringResource(R.string.a11y_reads_1), color = PrColors.Text, fontSize = 13.sp)
                    Text("• " + stringResource(R.string.a11y_reads_2), color = PrColors.Text, fontSize = 13.sp)
                    Label(stringResource(R.string.a11y_never_h))
                    Text("• " + stringResource(R.string.a11y_never_1), color = PrColors.Text, fontSize = 13.sp)
                    Text("• " + stringResource(R.string.a11y_never_2), color = PrColors.Text, fontSize = 13.sp)
                    Text("• " + stringResource(R.string.a11y_never_3), color = PrColors.Text, fontSize = 13.sp)
                    Hint(stringResource(R.string.a11y_stop))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    AccessibilityStatus.openAccessibilitySettings(context)
                }) { Text(stringResource(R.string.a11y_ok), color = PrColors.Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text(stringResource(R.string.a11y_cancel), color = PrColors.Muted) }
            },
        )
    }
    return { show = true }
}

@Composable
private fun DataPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var exportFailed by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(0) } // 1 = șterge tot, 2 = retrage acordul
    BackHeader(stringResource(R.string.data_title), stringResource(R.string.data_sub), onBack)
    PrCard {
        Text(stringResource(R.string.data_export), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
        Hint(stringResource(R.string.data_export_sub))
        PrimaryButton(stringResource(R.string.data_export_btn), onClick = { exportFailed = !DataRights.export(context) })
        if (exportFailed) Text(stringResource(R.string.data_export_fail), color = PrColors.Amber, fontSize = 13.sp)
    }
    PrCard {
        Text(stringResource(R.string.data_rides), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
        Hint(stringResource(R.string.data_rides_sub))
        SecondaryButton(stringResource(R.string.data_rides_btn), onClick = { DataRights.deleteRides() })
    }
    PrCard {
        Text(stringResource(R.string.data_all), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
        Hint(stringResource(R.string.data_all_sub))
        SecondaryButton(stringResource(R.string.data_all_btn), onClick = { confirm = 1 })
    }
    PrCard {
        Text(stringResource(R.string.data_withdraw), fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
        Hint(stringResource(R.string.data_withdraw_sub))
        SecondaryButton(stringResource(R.string.data_withdraw_btn), onClick = { confirm = 2 })
    }
    TipCard(stringResource(R.string.data_tip))
    if (confirm != 0) {
        val all = confirm == 1
        AlertDialog(
            onDismissRequest = { confirm = 0 },
            containerColor = PrColors.Surface,
            title = { Text(stringResource(if (all) R.string.data_all_confirm_title else R.string.data_withdraw), color = PrColors.Text, fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(if (all) R.string.data_all_confirm_body else R.string.data_withdraw_confirm), color = PrColors.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = 0
                    if (all) DataRights.deleteAll(context) else DataRights.withdrawConsent(context)
                }) { Text(stringResource(if (all) R.string.data_all_confirm_yes else R.string.data_withdraw_btn), color = PrColors.Amber, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = 0 }) { Text(stringResource(R.string.data_cancel), color = PrColors.Muted) }
            },
        )
    }
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    BackHeader(stringResource(R.string.legal_row_about), onBack = onBack)
    PrCard {
        Text("ProfitRide", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = PrColors.Text)
        Text(stringResource(R.string.about_tagline), color = PrColors.Muted, fontSize = 14.sp)
        KeyValueRow(stringResource(R.string.about_version), BuildConfig.VERSION_NAME)
        KeyValueRow(stringResource(R.string.about_terms), stringResource(R.string.legal_terms_version))
        KeyValueRow(stringResource(R.string.about_privacy), stringResource(R.string.legal_terms_version))
        KeyValueRow(stringResource(R.string.about_collected), stringResource(R.string.about_none))
    }
    PrCard {
        Label(stringResource(R.string.about_know_h))
        listOf(R.string.about_know_1, R.string.about_know_2, R.string.about_know_3).forEach {
            Text("• " + stringResource(it), color = PrColors.Text, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
    PrCard {
        Label(stringResource(R.string.about_contact))
        Text(stringResource(R.string.legal_operator), color = PrColors.Text, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.legal_email), color = PrColors.Muted, fontSize = 14.sp)
        Text(stringResource(R.string.legal_address), color = PrColors.Muted, fontSize = 14.sp)
    }
    PrCard {
        Label(stringResource(R.string.about_licences))
        Text(stringResource(R.string.about_licences_body), color = PrColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
