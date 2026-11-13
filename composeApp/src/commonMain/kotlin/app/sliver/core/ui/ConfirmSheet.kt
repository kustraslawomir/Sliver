package app.sliver.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

/** A destructive action asked once: title, one line, red confirm, Cancel. */
@Composable
fun ConfirmSheet(visible: Boolean, title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    SliverBottomSheet(visible, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            SliverText(title, SliverType.Title)
            SliverText(body, SliverType.BodySmall, color = SliverColors.TextSecondary)
            SecondaryButton(confirm, { onDismiss(); onConfirm() }, Modifier.fillMaxWidth(), destructive = true)
            GhostButton("Cancel", onDismiss, Modifier.fillMaxWidth())
        }
    }
}
