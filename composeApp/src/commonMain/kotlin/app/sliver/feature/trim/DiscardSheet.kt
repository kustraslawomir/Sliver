package app.sliver.feature.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SliverBottomSheet

/** Shown only for in-app recordings, which exist nowhere else once closed. */
@Composable
fun DiscardSheet(visible: Boolean, onKeep: () -> Unit, onDiscard: () -> Unit) {
    SliverBottomSheet(visible, onKeep) {
        Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            SliverText("Discard this recording?", SliverType.DisplayM)
            SliverText("It is not saved anywhere else.", SliverType.BodySmall)
            PrimaryButton("Keep editing", onKeep)
            GhostButton("Discard", onDiscard, destructive = true)
        }
    }
}
