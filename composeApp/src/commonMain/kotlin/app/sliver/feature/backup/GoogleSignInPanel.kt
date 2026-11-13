package app.sliver.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.data.auth.AccountManager
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/** "Back up your clips" sheet followed by the "Before Google consent" explainer. */
@Composable
fun GoogleSignInPanel(
    skipLabel: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onRestore: (() -> Unit)? = null,
) {
    val manager = remember { KoinPlatform.getKoin().get<AccountManager>() }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Owner: no explanation step — "Continue with Google" goes straight to Google's own sheet.
    fun signIn(restoreAfter: Boolean) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            manager.signIn()
                .onSuccess { if (restoreAfter && onRestore != null) onRestore() else onFinished() }
                .onFailure { error = "Sign-in did not finish. Try again." }
            busy = false
        }
    }

    Column(
        modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
    ) {
        SliverText("Back up your clips", SliverType.DisplayM)
        SliverText("Your clips go to a Fragments folder in your own Google Drive. Nothing else in your Drive is touched.", SliverType.Body, color = SliverColors.TextSecondary)
        if (manager.isFake) SliverText("Demo mode: no Google account is used.", SliverType.BodySmall)
        error?.let { SliverText(it, SliverType.BodySmall, color = SliverColors.Error) }
        Column(Modifier.padding(top = SliverSpace.S6), verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            PrimaryButton(if (busy) "Signing in…" else "Continue with Google", { signIn(restoreAfter = false) }, Modifier.fillMaxWidth(), enabled = !busy)
            GhostButton(skipLabel, onClick = onFinished)
            if (onRestore != null) GhostButton("Welcome back? Restore my clips", onClick = { signIn(restoreAfter = true) })
        }
    }
}
