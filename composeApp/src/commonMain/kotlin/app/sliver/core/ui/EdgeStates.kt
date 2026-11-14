package app.sliver.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sliver.core.APP_NAME
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

const val MIN_FREE_BYTES = 200L * 1024 * 1024

fun isStorageLow(freeBytes: Long): Boolean = freeBytes < MIN_FREE_BYTES

const val STORAGE_FULL_TITLE = "Phone storage is full"
const val STORAGE_FULL_BODY = "$APP_NAME needs about 200 MB free. Free up some space, then try again."

/** Centered message with an optional single action. */
@Composable
fun EdgeState(title: String, body: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {
}) {
    Column(
        modifier.padding(SliverSpace.Gutter),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SliverText(title, SliverType.DisplayM)
        SliverText(body, SliverType.BodySmall, color = SliverColors.TextSecondary)
        if (action != null) PrimaryButton(action, onAction, Modifier.fillMaxWidth())
    }
}

@Composable
fun StorageFullOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(SliverColors.BgBase).clickable(enabled = false) {}) {
        EdgeState(STORAGE_FULL_TITLE, STORAGE_FULL_BODY, Modifier.align(Alignment.Center), "OK", onDismiss)
    }
}
