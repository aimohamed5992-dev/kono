package sa.kono.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sa.kono.app.data.ApiError
import sa.kono.app.data.AppData
import sa.kono.app.data.AppRole
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoMark
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSize
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

/**
 * App-level authentication, shown once (after splash / onboarding) until the user signs in;
 * the session is then remembered across launches.
 */
private enum class Step { Welcome, Register, Otp }

const val DEMO_OTP = "1234"

@Composable
fun AuthFlow(onAuthenticated: () -> Unit) {
    var step by remember { mutableStateOf(Step.Welcome) }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(AppRole.Customer) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun verify() {
        scope.launch {
            try {
                AppData.completePhoneAuth(phone, name.trim().ifEmpty { null }, role)
                onAuthenticated()
            } catch (e: ApiError) {
                error = e.message ?: "حدث خطأ، حاول مرة أخرى."
                step = Step.Welcome
            }
        }
    }

    Box(Modifier.fillMaxSize().background(KonoColor.Background)) {
        AnimatedContent(
            step, label = "authStep",
            transitionSpec = {
                if (targetState == Step.Welcome) fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                else (slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)))
            },
        ) { s ->
            when (s) {
                Step.Welcome -> WelcomeScreen(
                    onLogin = { p -> phone = p; name = ""; role = AppRole.Customer; step = Step.Otp },
                    onRegister = { step = Step.Register },
                )
                Step.Register -> RegisterScreen(
                    onBack = { step = Step.Welcome },
                    onContinue = { n, p, r -> name = n; phone = p; role = r; step = Step.Otp },
                )
                Step.Otp -> OtpScreen(
                    phone = phone,
                    onVerified = ::verify,
                    onBack = { step = if (name.isNotEmpty()) Step.Register else Step.Welcome },
                )
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton({ error = null }) { Text("حسناً", style = KonoFont.Button, color = KonoColor.Brown) } },
            title = { Text("تعذّر تسجيل الدخول", style = KonoFont.Headline) },
            text = { Text(error ?: "", style = KonoFont.Body) },
            containerColor = KonoColor.Surface,
        )
    }
}

// MARK: - Welcome / login

@Composable
private fun WelcomeScreen(onLogin: (String) -> Unit, onRegister: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    val valid = phone.length == 9 && phone.first() == '5'
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KonoSpace.screen)
            .padding(top = KonoSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            KonoMark(Modifier.konoShadow(KonoShadow.Md, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)), size = 56.dp)
            Text("أهلاً بك في كونو", style = KonoFont.display(28), color = KonoColor.Espresso)
            Text("سجّل دخولك برقم جوالك، ولا تحتاج تسجّل كل مرة.", style = KonoFont.Body, color = KonoColor.TextSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
            Text("رقم الجوال", style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
            PhonePrefixField(phone) { phone = it }
            KonoButton("متابعة", { focus.clearFocus(); onLogin("+966$phone") }, enabled = valid)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("ما عندك حساب؟", style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
            Text("أنشئ حساب جديد", style = cairo(13, FontWeight.Bold), color = KonoColor.Brown,
                modifier = Modifier.clickable(onClick = onRegister))
        }
        Spacer(Modifier.weight(1f, fill = false).height(KonoSpace.xl))
        Text(
            "بمتابعتك فإنك توافق على الشروط والأحكام وسياسة الخصوصية.",
            Modifier.fillMaxWidth().padding(bottom = KonoSpace.l),
            style = KonoFont.Footnote, color = KonoColor.TextTertiary, textAlign = TextAlign.Center,
        )
    }
}

// MARK: - Register

@Composable
private fun RegisterScreen(onBack: () -> Unit, onContinue: (String, String, AppRole) -> Unit) {
    var role by remember { mutableStateOf(AppRole.Customer) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val valid = phone.length == 9 && phone.first() == '5' && name.trim().length >= 2
    val nameLabel = when (role) {
        AppRole.Customer -> "الاسم"
        AppRole.Merchant -> "اسم المتجر / المطعم"
        AppRole.Courier -> "اسم المندوب"
    }
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KonoSpace.screen)
            .padding(top = KonoSpace.s),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.xl),
    ) {
        BackButton(onBack)
        Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
            Text("حساب جديد", style = KonoFont.LargeTitle, color = KonoColor.Espresso)
            Text("اختر نوع الحساب وأدخل بياناتك.", style = KonoFont.Body, color = KonoColor.TextSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
            Text("نوع الحساب", style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                RolePill(AppRole.Customer, Icons.Filled.Person, role == AppRole.Customer, Modifier.weight(1f)) { role = AppRole.Customer }
                RolePill(AppRole.Merchant, Icons.Filled.Storefront, role == AppRole.Merchant, Modifier.weight(1f)) { role = AppRole.Merchant }
                RolePill(AppRole.Courier, Icons.Filled.DeliveryDining, role == AppRole.Courier, Modifier.weight(1f)) { role = AppRole.Courier }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text(nameLabel, style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
                KonoTextField(name, { name = it }, nameLabel, icon = Icons.Filled.Person)
            }
            Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text("رقم الجوال", style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
                PhonePrefixField(phone) { phone = it }
            }
        }
        KonoButton("متابعة", { focus.clearFocus(); onContinue(name.trim(), "+966$phone", role) }, enabled = valid)
        Text(
            "بإنشائك حساباً فإنك توافق على الشروط والأحكام وسياسة الخصوصية.",
            Modifier.fillMaxWidth().padding(bottom = KonoSpace.xl),
            style = KonoFont.Footnote, color = KonoColor.TextTertiary, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RolePill(role: AppRole, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg = if (selected) KonoColor.TextOnBrown else KonoColor.Brown
    Column(
        modifier
            .height(68.dp)
            .clip(KonoRadius.M)
            .background(if (selected) KonoColor.Brown else KonoColor.Surface)
            .border(BorderStroke(1.dp, if (selected) androidx.compose.ui.graphics.Color.Transparent else KonoColor.Hairline), KonoRadius.M)
            .konoClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = fg)
        Text(role.titleAr, style = cairo(12, FontWeight.Bold), color = fg)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .konoShadow(KonoShadow.Sm, CircleShape)
            .clip(CircleShape)
            .background(KonoColor.Surface)
            .konoClickable(pressedScale = 0.9f, onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        // ArrowBack is auto-mirrored: in RTL it points right, i.e. "back".
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", Modifier.size(20.dp), tint = KonoColor.Brown)
    }
}

// MARK: - Phone field

@Composable
private fun PhonePrefixField(value: String, onChange: (String) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        Row(
            Modifier
                .height(KonoSize.fieldHeight)
                .clip(KonoRadius.M)
                .background(KonoColor.Surface)
                .border(BorderStroke(1.dp, KonoColor.Hairline), KonoRadius.M)
                .padding(horizontal = KonoSpace.m),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🇸🇦", style = cairo(18))
            Text("\u200E+966", style = KonoFont.Headline, color = KonoColor.TextPrimary)
        }
        // The number itself is always typed left-to-right.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                Modifier
                    .weight(1f)
                    .height(KonoSize.fieldHeight)
                    .clip(KonoRadius.M)
                    .background(KonoColor.Surface)
                    .border(
                        BorderStroke(if (focused) 1.5.dp else 1.dp, if (focused) KonoColor.Brown else KonoColor.Hairline),
                        KonoRadius.M,
                    )
                    .padding(horizontal = KonoSpace.m),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text("5X XXX XXXX", style = KonoFont.Headline, color = KonoColor.TextTertiary)
                BasicTextField(
                    value = value,
                    onValueChange = { onChange(it.filter(Char::isDigit).take(9)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = KonoFont.Headline.copy(color = KonoColor.TextPrimary),
                    cursorBrush = SolidColor(KonoColor.Brown),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    interactionSource = source,
                )
            }
        }
    }
}

// MARK: - OTP

@Composable
private fun OtpScreen(phone: String, onVerified: () -> Unit, onBack: () -> Unit) {
    val length = 4
    var code by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(30) }
    var wrong by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    val focusReq = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { delay(450); focusReq.requestFocus() }
    LaunchedEffect(seconds) { if (seconds > 0) { delay(1000); seconds-- } }

    fun verify(c: String) {
        if (verifying) return
        verifying = true
        keyboard?.hide()
        scope.launch {
            delay(500)
            verifying = false
            // Prototype: the demo code — or any 4 digits — passes.
            if (c.length == length) onVerified()
            else {
                wrong = true; code = ""
                shake.snapTo(0f); shake.animateTo(1f, tween(350)); shake.snapTo(0f)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = KonoSpace.screen)
            .padding(top = KonoSpace.s),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
    ) {
        BackButton(onBack)
        Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
            KonoMark(Modifier.konoShadow(KonoShadow.Md, androidx.compose.foundation.shape.RoundedCornerShape(13.dp)), size = 48.dp)
            Text("رمز التحقق", style = KonoFont.LargeTitle, color = KonoColor.Espresso)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("أرسلنا رمزاً من ٤ أرقام إلى", style = KonoFont.Body, color = KonoColor.TextSecondary)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(phone, style = cairo(15, FontWeight.Bold), color = KonoColor.TextPrimary)
                }
            }
        }

        // demo code banner
        Row(
            Modifier
                .fillMaxWidth()
                .clip(KonoRadius.M)
                .background(KonoColor.Cream)
                .border(BorderStroke(1.dp, KonoColor.Brown.copy(alpha = 0.16f)), KonoRadius.M)
                .padding(KonoSpace.m),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoFixHigh, null, Modifier.size(20.dp), tint = KonoColor.Brown)
            Column(Modifier.weight(1f)) {
                Text("رمز التجربة", style = KonoFont.Caption, color = KonoColor.TextSecondary)
                Text(DEMO_OTP, style = cairo(22, FontWeight.ExtraBold).copy(letterSpacing = 4.sp), color = KonoColor.TextPrimary)
            }
            Text(
                "تعبئة تلقائية", style = cairo(11, FontWeight.Bold), color = KonoColor.TextOnBrown,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(KonoColor.Brown)
                    .konoClickable { code = DEMO_OTP; verify(DEMO_OTP) }
                    .padding(horizontal = KonoSpace.m, vertical = 8.dp),
            )
        }

        // OTP boxes (LTR) over a hidden text field
        Box(Modifier.fillMaxWidth().offset(x = (kotlin.math.sin(shake.value * Math.PI * 4) * 8).dp), contentAlignment = Alignment.Center) {
            BasicTextField(
                value = code,
                onValueChange = {
                    val v = it.filter(Char::isDigit).take(length)
                    code = v; wrong = false
                    if (v.length == length) verify(v)
                },
                modifier = Modifier.size(1.dp).focusRequester(focusReq),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = KonoFont.Body.copy(color = androidx.compose.ui.graphics.Color.Transparent),
                cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    repeat(length) { i ->
                        val filled = i < code.length
                        val active = i == code.length
                        Box(
                            Modifier
                                .size(width = 62.dp, height = 66.dp)
                                .clip(KonoRadius.M)
                                .background(KonoColor.Surface)
                                .border(
                                    BorderStroke(
                                        if (active) 2.dp else 1.2.dp,
                                        when {
                                            wrong -> KonoColor.Danger
                                            active || filled -> KonoColor.Brown
                                            else -> KonoColor.Brown.copy(alpha = 0.14f)
                                        },
                                    ), KonoRadius.M,
                                )
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    focusReq.requestFocus(); keyboard?.show()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (filled) code[i].toString() else "", style = cairo(26, FontWeight.ExtraBold), color = KonoColor.TextPrimary)
                        }
                    }
                }
            }
        }

        if (seconds > 0) {
            Text("إعادة إرسال الرمز خلال $seconds ثانية", style = KonoFont.Footnote, color = KonoColor.TextTertiary)
        } else {
            Text("إعادة إرسال الرمز", style = KonoFont.Subheadline, color = KonoColor.Brown,
                modifier = Modifier.clickable { seconds = 30 })
        }

        Spacer(Modifier.weight(1f))
        KonoButton("تأكيد", { verify(code) }, loading = verifying, enabled = code.length == length,
            modifier = Modifier.padding(bottom = KonoSpace.l))
    }
}
