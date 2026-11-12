package app.sliver.core.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton

/**
 * Shows the friendly pre-prompt until [kind] is usable, then [content].
 * [onDeniedAlternative] (e.g. "Add from gallery") is shown on the denied screen.
 */
@Composable
fun PermissionGate(
    kind: PermissionKind,
    controller: PermissionController = rememberPermissionController(),
    onSkip: (() -> Unit)? = null,
    onDeniedAlternative: (() -> Unit)? = null,
    content: @Composable (PermissionStatus) -> Unit,
) {
    val status = controller.status(kind)
    when {
        status.isUsable -> content(status)
        status == PermissionStatus.Denied -> PermissionDenied(kind, controller::openSettings, onDeniedAlternative)
        else -> PermissionPrePrompt(kind, { controller.request(kind) }, onSkip)
    }
}

@Composable
fun PermissionPrePrompt(kind: PermissionKind, onAllow: () -> Unit, onSkip: (() -> Unit)? = null) {
    val copy = kind.copy()
    PermissionLayout(copy.title, copy.body) {
        PrimaryButton(copy.allow, onAllow)
        if (onSkip != null) GhostButton("Not now", onSkip)
    }
}

@Composable
fun PermissionDenied(kind: PermissionKind, onOpenSettings: () -> Unit, onAlternative: (() -> Unit)? = null) {
    val (title, body) = when (kind) {
        PermissionKind.CameraMic -> "Camera access is off" to "Turn it on in Settings to record your clip, or add one from your gallery."
        PermissionKind.Notifications -> "Reminders are off" to "Turn notifications on in Settings to get your daily nudge."
        PermissionKind.CoarseLocation -> "Location is off" to "Turn on approximate location in Settings to see golden hour."
    }
    PermissionLayout(title, body) {
        PrimaryButton("Open Settings", onOpenSettings)
        if (onAlternative != null) SecondaryButton("Add from gallery", onAlternative)
    }
}

@Composable
private fun PermissionLayout(title: String, body: String, actions: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(SliverSpace.Gutter),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SliverText(title, SliverType.Title, color = SliverColors.TextPrimary)
        SliverText(body, SliverType.BodySmall, color = SliverColors.TextSecondary)
        actions()
    }
}
