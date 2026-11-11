package app.sliver.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

const val MIN_FREE_BYTES = 200L * 1024 * 1024
const val SPLASH_MS = 1400
const val SPLASH_FADE_MS = 240

fun isStorageLow(freeBytes: Long): Boolean = freeBytes < MIN_FREE_BYTES

const val STORAGE_FULL_TITLE = "Phone storage is full"
const val STORAGE_FULL_BODY = "Sliver needs about 200 MB free. Free up some space, then try again."

/** Centered message with an optional single action. */
@Composable
fun EdgeState(title: String, body: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
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

/** Black launch screen: an ember slit widens, the wordmark fades in, then the whole layer cross-fades away. */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!reduced) progress.animateTo(1f, SliverMotion.emphasized(SPLASH_MS))
        fade.animateTo(0f, SliverMotion.standard(SPLASH_FADE_MS))
        onFinished()
    }
    Box(Modifier.fillMaxSize().alpha(fade.value).background(SliverColors.BgVoid), contentAlignment = Alignment.Center) {
        val p = progress.value
        Box(Modifier.width(2.dp).height((220 * p).dp).clip(SliverShapes.Pill).background(SliverColors.Accent))
        SliverText(
            "SLIVER", SliverType.MonoL,
            Modifier.alpha(((p - 0.5f) * 2f).coerceIn(0f, 1f)).padding(top = 280.dp),
            SliverColors.TextPrimary,
        )
    }
}
