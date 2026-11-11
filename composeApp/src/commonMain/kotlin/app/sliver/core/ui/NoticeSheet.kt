package app.sliver.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

/** One-message sheet for failures the user should see; visible while [message] is non-null. */
@Composable
fun NoticeSheet(message: String?, onDismiss: () -> Unit) {
    SliverBottomSheet(message != null, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            SliverText(message.orEmpty(), SliverType.BodySmall)
            PrimaryButton("OK", onDismiss)
        }
    }
}
