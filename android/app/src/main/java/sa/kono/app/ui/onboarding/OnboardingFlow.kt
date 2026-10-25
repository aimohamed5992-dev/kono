package sa.kono.app.ui.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.data.AppRole
import sa.kono.app.data.CourierOnboarding
import sa.kono.app.data.MerchantOnboarding
import sa.kono.app.data.saudiCities
import sa.kono.app.data.submitCourierOnboarding
import sa.kono.app.data.submitMerchantOnboarding
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.customer.BottomActionBar
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import androidx.compose.foundation.border

/** Post-registration application for merchant / courier accounts. */
@Composable
fun OnboardingFlow(role: AppRole, onDone: () -> Unit) {
    when (role) {
        AppRole.Merchant -> MerchantWizard(onDone)
        AppRole.Courier -> CourierWizard(onDone)
        else -> onDone()
    }
}

@Composable
private fun WizardChrome(
    title: String, step: Int, total: Int, canNext: Boolean, isLast: Boolean, submitting: Boolean,
    onBack: () -> Unit, onNext: () -> Unit, onSkip: () -> Unit, content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp)) {
                if (step > 0) Box(
                    Modifier.size(38.dp).konoShadow(KonoShadow.Sm, CircleShape).clip(CircleShape).background(KonoColor.Surface).konoClickable(pressedScale = 0.9f, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", Modifier.size(20.dp), tint = KonoColor.Brown) }
            }
            Text(title, style = KonoFont.Headline, color = KonoColor.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("لاحقاً", style = KonoFont.Subheadline, color = KonoColor.TextSecondary, modifier = Modifier.konoClickable(onClick = onSkip))
        }
        Box(Modifier.padding(horizontal = KonoSpace.screen).padding(bottom = KonoSpace.m)) { OBProgress(step, total) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.xl)) { content() }
        BottomActionBar { KonoButton(if (isLast) "إرسال الطلب للمراجعة" else "التالي", onNext, loading = submitting, enabled = canNext) }
    }
}

/** Shown after a successful submit. */
@Composable
fun OnboardingSubmitted(role: AppRole, onContinue: () -> Unit) {
    val s by animateFloatAsState(1f, spring(0.55f, Spring.StiffnessLow), label = "pop")
    Column(Modifier.fillMaxSize().background(KonoColor.Background).navigationBarsPadding().padding(horizontal = KonoSpace.screen), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
        Box(Modifier.weight(1f))
        Box(Modifier.size(110.dp).scale(s).background(KonoColor.Success.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Verified, null, Modifier.size(52.dp), tint = KonoColor.Success)
        }
        Text("تم استلام طلبك", style = KonoFont.LargeTitle, color = KonoColor.Espresso)
        Text(
            "يراجع فريق كونو بياناتك ووثائقك خلال ٢٤–٤٨ ساعة عمل، وستصلك رسالة بالنتيجة. يمكنك الآن تجهيز ${if (role == AppRole.Merchant) "متجرك" else "ملفك"} بينما تكتمل المراجعة.",
            style = KonoFont.Body, color = KonoColor.TextSecondary, textAlign = TextAlign.Center,
        )
        Box(Modifier.weight(1f))
        KonoButton(if (role == AppRole.Merchant) "الذهاب إلى لوحة المتجر" else "الذهاب إلى ملف المندوب", onContinue, modifier = Modifier.padding(bottom = KonoSpace.xl))
    }
}

@Composable
private fun SummaryRow(k: String, v: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(k, style = KonoFont.Footnote, color = KonoColor.TextSecondary, modifier = Modifier.weight(1f))
        Text(v.ifBlank { "—" }, style = KonoFont.Callout, color = KonoColor.TextPrimary, textAlign = TextAlign.End)
    }
}

@Composable
private fun DocCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(horizontal = KonoSpace.m)) { content() }
}

// ── Courier ─────────────────────────────────────────────────────────────────

@Composable
fun CourierWizard(onDone: () -> Unit) {
    var ob by remember { mutableStateOf(CourierOnboarding()) }
    var step by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val total = 5

    if (submitted) { OnboardingSubmitted(AppRole.Courier, onDone); return }

    val canNext = when (step) { 0 -> ob.personalDone; 1 -> ob.vehicleDone; 2 -> ob.docsDone; 3 -> ob.bankDone; else -> ob.canSubmit }
    WizardChrome("تسجيل مندوب", step, total, canNext, step == total - 1, submitting, onBack = { step-- }, onSkip = onDone, onNext = {
        if (step < total - 1) step++ else { submitting = true; scope.launch { if (AppData.submitCourierOnboarding(ob)) submitted = true else error = true; submitting = false } }
    }) {
        when (step) {
            0 -> OBSection("البيانات الشخصية", "كما هي في الهوية / الإقامة") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBField("الاسم الكامل", ob.fullNameAr, { ob = ob.copy(fullNameAr = it) }, true, Icons.Filled.Person)
                    OBField("رقم الهوية / الإقامة", ob.nationalId, { ob = ob.copy(nationalId = it.filter(Char::isDigit).take(10)) }, true, Icons.Filled.Badge, KeyboardType.Number, "١٠ أرقام")
                    OBDateField("تاريخ الميلاد", ob.dateOfBirth, { ob = ob.copy(dateOfBirth = it) }, true)
                    OBPicker("الجنسية", ob.nationality, { ob = ob.copy(nationality = it) }, CourierOnboarding.nationalities.map { it to it }, true)
                    OBPicker("الجنس", ob.gender, { ob = ob.copy(gender = it) }, listOf("ذكر" to "ذكر", "أنثى" to "أنثى"))
                    OBField("البريد الإلكتروني", ob.email, { ob = ob.copy(email = it) }, icon = Icons.Filled.Email, keyboard = KeyboardType.Email)
                    OBPicker("المدينة", ob.cityAr, { ob = ob.copy(cityAr = it) }, saudiCities.map { it to it }, true)
                    OBField("العنوان الوطني (الرمز المختصر)", ob.nationalAddress, { ob = ob.copy(nationalAddress = it) }, icon = Icons.Filled.Place)
                }
            }
            1 -> OBSection("بيانات المركبة", "وسيلة التوصيل التي ستستخدمها") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBPicker("نوع وسيلة التوصيل", ob.vehicleTypeAr, { ob = ob.copy(vehicleTypeAr = it) }, CourierOnboarding.vehicleTypes.map { it to it }, true)
                    if (!ob.walking) {
                        OBField("الشركة المصنّعة", ob.vehicleMake, { ob = ob.copy(vehicleMake = it) }, true, Icons.Filled.Build, hint = "هوندا، ياماها، هيونداي…")
                        OBField("الطراز", ob.vehicleModel, { ob = ob.copy(vehicleModel = it) }, true)
                        OBField("سنة الصنع", ob.vehicleYear?.toString().orEmpty(), { ob = ob.copy(vehicleYear = it.filter(Char::isDigit).take(4).toIntOrNull()) }, icon = Icons.Filled.CalendarMonth, keyboard = KeyboardType.Number)
                        OBField("اللون", ob.vehicleColorAr, { ob = ob.copy(vehicleColorAr = it) })
                        OBField("رقم اللوحة", ob.plateAr, { ob = ob.copy(plateAr = it) }, true, Icons.Filled.Numbers, hint = "أ ب ج ١٢٣٤")
                        OBPicker("نوع اللوحة", ob.plateType, { ob = ob.copy(plateType = it) }, CourierOnboarding.plateTypes.map { it to it })
                    }
                    OBField("رقم تصريح توصيل الطلبات (الهيئة العامة للنقل)", ob.workPermitNumber, { ob = ob.copy(workPermitNumber = it) }, icon = Icons.Filled.VerifiedUser)
                    if (!ob.walking) {
                        OBField("رقم رخصة القيادة", ob.licenseNumber, { ob = ob.copy(licenseNumber = it) }, icon = Icons.Filled.CreditCard)
                        OBDateField("تاريخ انتهاء الرخصة", ob.licenseExpiry, { ob = ob.copy(licenseExpiry = it) }, kind = DateKind.Expiry)
                    }
                }
            }
            2 -> OBSection("الوثائق المطلوبة", "صوّر كل وثيقة بوضوح — يقبل النظام JPG/PNG") {
                DocCard {
                    val visible = ob.documents.filter { !(it.type in setOf("vehicle_reg", "vehicle_insurance", "driving_license") && ob.walking) }
                    visible.forEachIndexed { i, d ->
                        OBDocRow(d) { nd -> ob = ob.copy(documents = ob.documents.map { if (it.type == nd.type) nd else it }) }
                        if (i < visible.lastIndex) androidx.compose.material3.Divider(color = KonoColor.Divider)
                    }
                }
            }
            3 -> OBSection("الحساب البنكي", "لإيداع أرباحك أسبوعياً — يجب أن يطابق اسم صاحب الحساب هويتك") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBField("رقم الآيبان (IBAN)", ob.iban, { ob = ob.copy(iban = it.uppercase()) }, true, Icons.Filled.AccountBalance, hint = "SA................")
                    OBPicker("اسم البنك", ob.bankNameAr, { ob = ob.copy(bankNameAr = it) }, CourierOnboarding.banks.map { it to it }, true)
                    OBField("اسم صاحب الحساب", ob.accountHolderAr, { ob = ob.copy(accountHolderAr = it) }, true, Icons.Filled.AccountCircle)
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
                OBSection("مراجعة وإرسال") {
                    Column {
                        SummaryRow("الاسم", ob.fullNameAr); SummaryRow("الهوية", ob.nationalId); SummaryRow("وسيلة التوصيل", ob.vehicleTypeAr)
                        if (!ob.walking) SummaryRow("المركبة", "${ob.vehicleMake} ${ob.vehicleModel} · ${ob.plateAr}")
                        SummaryRow("الوثائق المرفقة", "${ob.documents.count { it.provided }} من ${ob.documents.size}"); SummaryRow("البنك", ob.bankNameAr)
                    }
                }
                OBConsent(ob.agreeTerms) { ob = ob.copy(agreeTerms = !ob.agreeTerms) }
            }
        }
    }
    if (error) SubmitError { error = false }
}

@Composable
private fun SubmitError(onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = { TextButton(onDismiss) { Text("حسناً", style = KonoFont.Button, color = KonoColor.Brown) } },
    title = { Text("تعذّر إرسال الطلب", style = KonoFont.Headline) },
    text = { Text("تحقّق من اتصالك وحاول مرة أخرى.", style = KonoFont.Body) },
    containerColor = KonoColor.Surface,
)

// ── Merchant ────────────────────────────────────────────────────────────────

@Composable
fun MerchantWizard(onDone: () -> Unit) {
    var ob by remember { mutableStateOf(MerchantOnboarding()) }
    var step by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val total = 6

    if (submitted) { OnboardingSubmitted(AppRole.Merchant, onDone); return }

    val canNext = when (step) { 0 -> ob.businessDone; 1 -> ob.locationDone; 2 -> ob.docsDone; 3 -> ob.bankDone; 4 -> ob.contactDone; else -> ob.canSubmit }
    WizardChrome("تسجيل متجر", step, total, canNext, step == total - 1, submitting, onBack = { step-- }, onSkip = onDone, onNext = {
        if (step < total - 1) step++ else { submitting = true; scope.launch { if (AppData.submitMerchantOnboarding(ob)) submitted = true else error = true; submitting = false } }
    }) {
        when (step) {
            0 -> OBSection("بيانات المنشأة", "كما هي في السجل التجاري") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBField("الاسم التجاري / اسم العلامة", ob.tradeNameAr, { ob = ob.copy(tradeNameAr = it) }, true, Icons.Filled.Storefront)
                    OBPicker("نوع النشاط", ob.activityKind, { ob = ob.copy(activityKind = it) }, MerchantOnboarding.activities.distinctBy { it.first }.map { it.first to it.second }, true)
                    OBField("وصف مختصر / التصنيف", ob.cuisineAr, { ob = ob.copy(cuisineAr = it) }, icon = Icons.Filled.Category, hint = "برجر · أمريكي")
                    OBField("رقم السجل التجاري", ob.crNumber, { ob = ob.copy(crNumber = it.filter(Char::isDigit).take(10)) }, true, Icons.Filled.Description, KeyboardType.Number, "١٠ أرقام")
                    OBDateField("تاريخ انتهاء السجل التجاري", ob.crExpiry, { ob = ob.copy(crExpiry = it) }, true, DateKind.Expiry)
                    OBField("الرقم الضريبي (VAT)", ob.vatNumber, { ob = ob.copy(vatNumber = it.filter(Char::isDigit).take(15)) }, icon = Icons.Filled.Percent, keyboard = KeyboardType.Number, hint = "١٥ رقم — إن وُجد")
                    OBField("نبذة عن المتجر", ob.descriptionAr, { ob = ob.copy(descriptionAr = it) }, icon = Icons.Filled.Notes)
                }
            }
            1 -> OBSection("الفرع والموقع") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBPicker("المدينة", ob.cityAr, { ob = ob.copy(cityAr = it) }, saudiCities.map { it to it }, true)
                    OBField("الحي", ob.districtAr, { ob = ob.copy(districtAr = it) }, true, Icons.Filled.Map)
                    OBField("الشارع / العنوان التفصيلي", ob.streetAr, { ob = ob.copy(streetAr = it) }, true, Icons.Filled.Signpost)
                    OBField("العنوان الوطني", ob.nationalAddress, { ob = ob.copy(nationalAddress = it) }, icon = Icons.Filled.Place)
                    OBField("رقم هاتف الفرع", ob.branchPhone, { ob = ob.copy(branchPhone = it) }, true, Icons.Filled.Phone, KeyboardType.Phone)
                    OBField("ساعات العمل", ob.openingHoursAr, { ob = ob.copy(openingHoursAr = it) }, true, Icons.Filled.Schedule)
                    OBMapPicker("الموقع على الخريطة", ob.latitude, ob.longitude, { la, lo -> ob = ob.copy(latitude = la, longitude = lo) }, true)
                }
            }
            2 -> OBSection("الوثائق والتراخيص", if (ob.activityKind == "Pharmacy") "الصيدليات تتطلب ترخيص هيئة الغذاء والدواء" else "صوّر كل وثيقة بوضوح") {
                DocCard {
                    val visible = ob.documents.filter { !(it.type == "sfda_license" && ob.activityKind != "Pharmacy") }
                    visible.forEachIndexed { i, d ->
                        OBDocRow(d) { nd -> ob = ob.copy(documents = ob.documents.map { if (it.type == nd.type) nd else it }) }
                        if (i < visible.lastIndex) androidx.compose.material3.Divider(color = KonoColor.Divider)
                    }
                }
            }
            3 -> OBSection("الشعار والحساب البنكي") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBImageCard("شعار المتجر", ob.logoDataUri, { ob = ob.copy(logoDataUri = it) }, true)
                    OBField("رقم الآيبان (IBAN)", ob.iban, { ob = ob.copy(iban = it.uppercase()) }, true, Icons.Filled.AccountBalance, hint = "SA................")
                    OBPicker("اسم البنك", ob.bankNameAr, { ob = ob.copy(bankNameAr = it) }, CourierOnboarding.banks.map { it to it }, true)
                    OBField("اسم صاحب الحساب (يطابق السجل التجاري)", ob.accountHolderAr, { ob = ob.copy(accountHolderAr = it) }, true, Icons.Filled.AccountCircle)
                }
            }
            4 -> OBSection("الإعدادات وجهة الاتصال") {
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    OBPicker("من يقوم بالتوصيل", ob.deliveryBy, { ob = ob.copy(deliveryBy = it) }, MerchantOnboarding.deliveryOptions.map { it to "مندوبو $it" })
                    OBField("اسم مسؤول التواصل", ob.contactNameAr, { ob = ob.copy(contactNameAr = it) }, true, Icons.Filled.Person)
                    OBField("جوال مسؤول التواصل", ob.contactPhone, { ob = ob.copy(contactPhone = it) }, true, Icons.Filled.Phone, KeyboardType.Phone)
                    OBField("بريد مسؤول التواصل", ob.contactEmail, { ob = ob.copy(contactEmail = it) }, icon = Icons.Filled.Email, keyboard = KeyboardType.Email)
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
                OBSection("مراجعة وإرسال") {
                    Column {
                        SummaryRow("المتجر", ob.tradeNameAr)
                        SummaryRow("النشاط", MerchantOnboarding.activities.firstOrNull { it.first == ob.activityKind }?.second ?: ob.activityKind)
                        SummaryRow("السجل التجاري", ob.crNumber); SummaryRow("الموقع", "${ob.districtAr}، ${ob.cityAr}")
                        SummaryRow("الوثائق المرفقة", "${ob.documents.count { it.provided }} من ${ob.documents.size}"); SummaryRow("البنك", ob.bankNameAr)
                    }
                }
                OBConsent(ob.agreeTerms) { ob = ob.copy(agreeTerms = !ob.agreeTerms) }
            }
        }
    }
    if (error) SubmitError { error = false }
}
