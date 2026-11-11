package app.sliver.feature.looks

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SliverSlider
import app.sliver.data.LookParams
import app.sliver.data.LookPreset
import app.sliver.data.info
import app.sliver.data.toParams
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.trim.TrimMedia
import app.sliver.feature.trim.TrimPlayer
import app.sliver.feature.trim.rememberPhoto
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

private const val PREVIEW_EDGE = 1080
private const val THUMB_EDGE = 96

@Composable
fun LooksScreen(onBack: () -> Unit) {
    val path = remember { app.sliver.feature.camera.CaptureSession.path }
    if (path == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val koin = remember { KoinPlatform.getKoin() }
    val settings = remember { koin.get<SettingsRepository>() }
    val media = remember { koin.get<TrimMedia>() }
    val renderer = rememberLookRenderer()
    val scope = rememberCoroutineScope()

    val isPhoto = remember { LookSession.isPhoto }
    var params by remember { mutableStateOf(LookSession.params) }
    var tab by remember { mutableStateOf(0) }
    var tool by remember { mutableStateOf(AdjustTool.SHARPNESS) }
    var showOriginal by remember { mutableStateOf(false) }

    val photo = if (isPhoto) rememberPhoto(path) else null
    val frame by produceState<ImageBitmap?>(null, path) {
        value = if (isPhoto) null else runCatching { media.thumbnails(path, media.durationMs(path), 1).firstOrNull() }.getOrNull()
    }
    val still = photo ?: frame

    val thumbs by produceState<Map<LookPreset, ImageBitmap>>(emptyMap(), still, params.intensity) {
        val f = still ?: return@produceState
        value = LookPreset.entries.associateWith {
            renderer.render(f, it.toParams(params.intensity), THUMB_EDGE)
        }
    }
    val photoShown by produceState<ImageBitmap?>(null, photo, params, showOriginal) {
        val p = photo ?: return@produceState
        value = renderer.render(p, if (showOriginal) LookParams() else params, PREVIEW_EDGE)
    }

    Column(Modifier.fillMaxSize().background(SliverColors.BgVoid).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
            SliverText("FILTERS", SliverType.MonoS, color = SliverColors.TextSecondary)
            SliverText(
                if (showOriginal) "ORIGINAL" else "HOLD = ORIGINAL", SliverType.MonoS,
                color = if (showOriginal) SliverColors.Accent else SliverColors.TextTertiary,
                modifier = Modifier.padding(end = SliverSpace.S3),
            )
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2)
                .clip(SliverShapes.S).background(SliverColors.Surface1)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        showOriginal = true
                        tryAwaitRelease()
                        showOriginal = false
                    })
                },
        ) {
            if (isPhoto) {
                (photoShown ?: photo)?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            } else {
                TrimPlayer(
                    path, LookSession.startMs, LookSession.windowMs, LookSession.speed, audio = false, paused = false,
                    modifier = Modifier.fillMaxSize(), look = if (showOriginal) LookParams() else params,
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            Segmented(listOf("FILTERS", "ADJUST"), tab, { tab = it })
            if (tab == 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    items(LookPreset.entries) { preset ->
                        LookThumb(preset, thumbs[preset], preset == params.preset) { params = params.withPreset(preset) }
                    }
                }
                if (params.preset.info.lutFile != null) {
                    ValueRow("Intensity", "${params.intensity}", onReset = { params = params.copy(intensity = 100) })
                    SliverSlider(params.intensity / 100f, { params = params.copy(intensity = Math.round(it * 100)) })
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
                ) {
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        items(AdjustTool.entries) { t ->
                            Chip(t.label, tool == t, { tool = t })
                        }
                    }
                }
                ValueRow(
                    tool.label, "${params.value(tool)}",
                    onReset = { params = params.reset(tool) },
                    resetEnabled = params.isModified(tool),
                )
                SliverSlider(
                    tool.toFraction(params.value(tool)),
                    { params = params.with(tool, tool.fromFraction(it)) },
                    Modifier.padding(vertical = SliverSpace.S2),
                )
                GhostButton("Reset all", { params = params.resetAll() }, Modifier.fillMaxWidth())
            }
            PrimaryButton("Apply filter", {
                LookSession.applied = params
                onBack()
            })
            GhostButton("Set as default filter", {
                scope.launch { settings.update { it.copy(defaultLook = params.preset) } }
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String, onReset: () -> Unit, resetEnabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onDoubleTap = { onReset() }) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SliverText(label.uppercase(), SliverType.MonoS, color = SliverColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3), verticalAlignment = Alignment.CenterVertically) {
            SliverText(value, SliverType.MonoS, color = SliverColors.TextPrimary)
            SliverText(
                "RESET", SliverType.MonoS,
                color = if (resetEnabled) SliverColors.Accent else SliverColors.TextTertiary,
                modifier = Modifier.clickable(enabled = resetEnabled, onClick = onReset).padding(vertical = SliverSpace.S2),
            )
        }
    }
}

@Composable
private fun LookThumb(preset: LookPreset, thumb: ImageBitmap?, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(64.dp, 80.dp).clip(SliverShapes.S).background(SliverColors.Surface2)
                .border(if (selected) 2.dp else 0.dp, if (selected) SliverColors.Accent else SliverColors.Surface2, SliverShapes.S),
        ) {
            thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        SliverText(
            preset.info.label.uppercase(), SliverType.MonoS,
            color = if (selected) SliverColors.Accent else SliverColors.TextSecondary,
            modifier = Modifier.padding(top = SliverSpace.S1),
        )
    }
}
