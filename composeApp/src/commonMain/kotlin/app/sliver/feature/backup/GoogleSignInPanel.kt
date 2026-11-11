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

val ConsentPoints = listOf(
    "Google will ask to let SLIVER create one visible folder in your Drive named SLIVER.",
    "SLIVER can only see files it creates itself, never the rest of your Drive.",
    "We never store your videos on our servers. They go straight to your Drive.",
    "Each second is about 2 MB.",
)

/** "Save your seconds" sheet followed by the "Before Google consent" explainer. */
@Composable
fun GoogleSignInPanel(
    skipLabel: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onRestore: (() -> Unit)? = null,
) {
    var restoreAfter by remember { mutableStateOf(false) }
    val manager = remember { KoinPlatform.getKoin().get<AccountManager>() }
    val scope = rememberCoroutineScope()
    var explainer by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
    ) {
        if (!explainer) {
            SliverText("Save your seconds", SliverType.DisplayM)
            SliverText("Back up to your own Google Drive, or stay a guest.", SliverType.Body, color = SliverColors.TextSecondary)
        } else {
            SliverText("Before Google asks", SliverType.DisplayM)
            ConsentPoints.forEach { SliverText(it, SliverType.Body, color = SliverColors.TextSecondary) }
            if (manager.isFake) SliverText("Demo mode: no Google account is used.", SliverType.BodySmall)
            error?.let { SliverText(it, SliverType.BodySmall, color = SliverColors.Error) }
        }
        Column(Modifier.padding(top = SliverSpace.S6), verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            PrimaryButton(
                if (explainer) "Continue" else "Continue with Google",
                {
                    if (!explainer) explainer = true
                    else if (!busy) {
                        busy = true
                        error = null
                        scope.launch {
                            manager.signIn().onSuccess { if (restoreAfter && onRestore != null) onRestore() else onFinished() }.onFailure { error = "Sign-in did not finish. Try again." }
                            busy = false
                        }
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            GhostButton(
                if (explainer) "Back" else skipLabel,
                onClick = { if (explainer) { explainer = false; restoreAfter = false } else onFinished() },
            )
            if (!explainer && onRestore != null) {
                GhostButton("Welcome back? Restore my seconds", onClick = { restoreAfter = true; explainer = true })
            }
        }
    }
}
