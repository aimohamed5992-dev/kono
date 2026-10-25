package sa.kono.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.data.AppRole
import sa.kono.app.data.needsOnboarding
import sa.kono.app.ui.onboarding.OnboardingFlow
import sa.kono.app.ui.customer.CustomerRoot
import sa.kono.app.ui.merchant.MerchantRoot
import sa.kono.app.ui.courier.CourierRoot
import sa.kono.app.data.KonoApi
import sa.kono.app.ui.OnboardingScreen
import sa.kono.app.ui.auth.AuthFlow
import sa.kono.app.ui.components.KonoSplash
import sa.kono.app.ui.theme.KonoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KonoApi.init(this)
        sa.kono.app.data.RecentSearches.init(this)
        sa.kono.app.ui.i18n.AppLanguage.init(this)
        setContent { KonoTheme(rtl = sa.kono.app.ui.i18n.AppLanguage.isArabic) { RootFlow() } }
    }
}

private enum class Phase { Splash, Onboarding, Auth, Applying, Running }

/** splash → onboarding (first launch only) → auth → role experience. Logging out restarts at the splash. */
@Composable
private fun RootFlow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("kono.app", Context.MODE_PRIVATE) }
    var phase by rememberSaveable { mutableStateOf(Phase.Splash) }
    val scope = rememberCoroutineScope()

    fun resolveEntry() = scope.launch {
        phase = when {
            AppData.restoreSession() -> if (AppData.needsOnboarding) Phase.Applying else Phase.Running
            !prefs.getBoolean("hasOnboarded", false) -> Phase.Onboarding
            else -> Phase.Auth
        }
    }

    AnimatedContent(
        phase, label = "root",
        transitionSpec = {
            if (targetState == Phase.Auth || targetState == Phase.Applying || targetState == Phase.Running)
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith fadeOut()
            else fadeIn() togetherWith fadeOut()
        },
    ) { p ->
        when (p) {
            Phase.Splash -> KonoSplash { resolveEntry() }
            Phase.Onboarding -> OnboardingScreen {
                prefs.edit().putBoolean("hasOnboarded", true).apply()
                phase = Phase.Auth
            }
            Phase.Auth -> AuthFlow { phase = if (AppData.needsOnboarding) Phase.Applying else Phase.Running }
            Phase.Applying -> OnboardingFlow(AppData.role) { phase = Phase.Running }
            Phase.Running -> {
                // Logout restarts the whole entry journey: splash → onboarding → auth.
                val signOut = {
                    AppData.signOut()
                    prefs.edit().putBoolean("hasOnboarded", false).apply()
                    phase = Phase.Splash
                }
                when (AppData.role) {
                    AppRole.Customer -> CustomerRoot(signOut)
                    AppRole.Merchant -> MerchantRoot(signOut)
                    AppRole.Courier -> CourierRoot(signOut)
                }
            }
        }
    }
}
