package app.sliver.feature.trim

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import app.sliver.core.navigation.LocalNavigator
import app.sliver.core.navigation.Route
import app.sliver.core.ui.PredictiveBackHandler
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.data.DaySource
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.toParams
import app.sliver.data.export.PhotoMotion
import app.sliver.data.export.SECOND_MS
import app.sliver.data.files.FileStore
import app.sliver.data.FilmFormat
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.autoimport.CandidateStrip
import app.sliver.feature.autoimport.MotionChoice
import app.sliver.feature.autoimport.rememberDraftNav
import app.sliver.feature.camera.CaptureSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import kotlin.time.Clock


/** Decodes a downsampled bitmap of the still at [path] (file path or content URI). */
@Composable
expect fun rememberPhoto(path: String): androidx.compose.ui.graphics.ImageBitmap?

@Composable
fun PhotoTrimScreen(onNavigate: (Route) -> Unit) {
    val path = remember { CaptureSession.path }
    if (path == null) {
        LaunchedEffect(Unit) { onNavigate(Route.Camera) }
        return
    }
    val koin = remember { KoinPlatform.getKoin() }
    val undo = remember { koin.get<SaveUndo>() }
    val draft = remember { CaptureSession.draft }
    val motionPhoto = remember { CaptureSession.motionPhoto }
    val draftNav = rememberDraftNav()
    val files = remember { koin.get<FileStore>() }
    val projects = remember { koin.get<ProjectRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }
    val day = remember { CaptureSession.forDate ?: today }
    var motion by remember { mutableStateOf(Motion.STILL) }
    var saving by remember { mutableStateOf(false) }
    val project by remember { projects.active }.collectAsState(null)
    var done by remember { mutableStateOf(false) }
    val photo = rememberPhoto(path)


    DisposableEffect(Unit) {
        onDispose { if (!done && isTmp(path)) files.delete(path) }
    }

    val progress by rememberInfiniteTransition().animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(SECOND_MS.toInt(), easing = LinearEasing), RepeatMode.Restart),
    )

    val navigator = LocalNavigator.current
    val inApp = remember { CaptureSession.source == DaySource.CAMERA }
    var discardOpen by remember { mutableStateOf(false) }

    fun close() {
        if (inApp) discardOpen = true else navigator.back()
    }

    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = inApp && !discardOpen, onProgress = { backProgress = it }, onBack = { backProgress = 0f; close() })

    fun useSecond() {
        if (saving) return
        saving = true
        scope.launch {
            val format = project?.format ?: FilmFormat.CINEMA
            val request = SaveClipRequest(
                date = day, source = path, startMs = 0, speed = 1f, cropY = null, format = format,
                daySource = DaySource.PHOTO, takenAt = Clock.System.now(), motion = motion,
                look = LookParams(),
            )
            val ok = (if (draft != null) draftNav.keep(draft, request) else runCatching { undo.save(request) }).isSuccess
            saving = false
            if (ok) {
                done = true
                if (draft != null) draftNav.afterResolved(day) { navigator.finishFlow(day) } else navigator.finishFlow(day)
            }
        }
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer { (1f - 0.08f * backProgress).let { scaleX = it; scaleY = it } }
            .background(SliverColors.BgVoid),
    ) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton44(::close, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
            SliverText("For ${dayLabel(day, today).lowercase()}", SliverType.Meta, color = SliverColors.TextSecondary)
            Box(Modifier.padding(SliverSpace.S6))
        }
        ClipFrame(
            if (project?.format == FilmFormat.STORY) 9f / 16f else 16f / 9f,
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2)
                .clip(SliverShapes.S).clipToBounds(),
        ) {
            photo?.let {
                val scale = if (motion == Motion.ZOOM) PhotoMotion.zoomScale(progress) else 1f
                Image(
                    it, null, Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            if (draft != null) CandidateStrip(draft, draftNav)
            if (motionPhoto) MotionChoice(video = false, draftNav)
            Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                Chip("Still", motion == Motion.STILL, { motion = Motion.STILL })
                Chip("Slow zoom", motion == Motion.ZOOM, { motion = Motion.ZOOM })
            }
            PrimaryButton(
                if (saving) "Saving..." else if (draft != null) "Keep this clip" else "Save clip", ::useSecond,
                enabled = photo != null && !saving && !draftNav.busy,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                if (draft != null) {
                    GhostButton("Remove day", { if (!saving) draftNav.skip(draft) }, Modifier.weight(1f), destructive = true, enabled = !saving && !draftNav.busy)
                } else {
                    GhostButton(
                        if (inApp) "Retake" else "Pick another",
                        { if (inApp) onNavigate(Route.Camera) else navigator.back() },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
    DiscardSheet(discardOpen, { discardOpen = false }, { onNavigate(Route.Camera) })
    }
}
