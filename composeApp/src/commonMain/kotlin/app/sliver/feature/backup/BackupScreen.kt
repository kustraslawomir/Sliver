package app.sliver.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.auth.AccountManager
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/** Account entry point. Drive backup itself arrives with the backup task. */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val repo = remember { koin.get<SettingsRepository>() }
    val manager = remember { koin.get<AccountManager>() }
    val scope = rememberCoroutineScope()
    val settings by remember { repo.settings }.collectAsState(null)
    val account = settings?.account

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
    ) {
        GhostButton("Back", onClick = onBack)
        when {
            account == null -> {}
            account.signedIn -> Column(
                Modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
                verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
            ) {
                SliverText("Backup & account", SliverType.DisplayM)
                SliverText("Signed in with Google", SliverType.Body, color = SliverColors.TextSecondary)
                SliverText(account.email ?: "", SliverType.Body)
                SliverText("What we keep: your account email. Your videos: never.", SliverType.BodySmall)
                SecondaryButton("Sign out", { scope.launch { manager.signOut() } }, Modifier.fillMaxWidth())
            }
            else -> GoogleSignInPanel(skipLabel = "Not now", onFinished = onBack)
        }
    }
}
