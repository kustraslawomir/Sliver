package app.sliver.feature.films

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.SliverSlider
import app.sliver.core.ui.SwitchRow
import kotlinx.datetime.LocalDate

class MixClip(val date: LocalDate, val keepAudio: Boolean, val thumb: String? = null, val color: Int = 0)

/**
 * v1.9 §1 sound sheet: a real timeline (clips filmstrip + one labelled, tinted bar per layer + playhead; tap a clip to
 * mute it, tap a bar to edit the layer), the clips' own volume, "Muted clips · N", the layers with a preview button,
 * and "+ Add music".
 */
@Composable
fun AudioMixSection(
    mix: AudioMix,
    onMix: (AudioMix) -> Unit,
    clips: List<MixClip>,
    filmMs: Long,
    playheadMs: () -> Long,
    onAddLayer: () -> Unit,
    onEditLayer: (Int) -> Unit,
    onMutedClips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    app.sliver.core.log.RecomposeProbe("soundSheet")
    val preview = rememberTrackPreview()
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText("Sound", SliverType.Title)
        MixTimeline(mix, onMix, clips, filmMs, playheadMs, onEditLayer)

        val clipsMuted = mix.clipVolume == 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            SliverText("Clip sound", SliverType.Body, Modifier.weight(1f))
            SliverText(
                if (clipsMuted) "Muted" else "${mix.clipVolume} %",
                ReelogType.label.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"),
                color = SliverColors.TextSecondary
            )
            IconButton44(
                { onMix(mix.copy(clipVolume = if (clipsMuted) 100 else 0)) },
                if (clipsMuted) "Unmute all clips" else "Mute all clips",
            ) { SliverIcon(if (clipsMuted) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.TextPrimary, size = 20.dp) }
        }
        SliverSlider(mix.clipVolume / 100f, { onMix(mix.copy(clipVolume = (it * 100).toInt().coerceIn(0, 100))) })
        if (clips.any { it.keepAudio }) {
            val muted = clips.count { it.keepAudio && it.date.toString() in mix.mutedClips }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(SliverShapes.R12).clickable(role = Role.Button, onClick = onMutedClips),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SliverText("Muted clips", SliverType.Body, Modifier.weight(1f))
                SliverText("$muted", ReelogType.label.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"), color = SliverColors.TextSecondary)
                SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary, Modifier.padding(start = SliverSpace.S2), size = 18.dp)
            }
        }

        Box(Modifier.fillMaxWidth().padding(vertical = SliverSpace.S1).height(1.dp).background(SliverColors.LineHairline))
        if (mix.layers.isEmpty()) {
            SliverText("Add music from your files. It plays over the clips.", SliverType.BodySmall, color = SliverColors.TextSecondary)
        }
        mix.layers.forEachIndexed { i, layer -> LayerRow(i, layer, filmMs, mix, onMix, onEditLayer, preview) }
        app.sliver.core.kit.ReelogButton("Add music", onAddLayer, kind = app.sliver.core.kit.ButtonKind.Secondary, leading = SliverIcons.Plus)
        SliverText("You're responsible for music you add from your files.", SliverType.Caption, color = SliverColors.TextTertiary)
    }
}

@Composable
private fun LayerRow(i: Int, layer: AudioLayer, filmMs: Long, mix: AudioMix, onMix: (AudioMix) -> Unit, onEditLayer: (Int) -> Unit, preview: TrackPreview) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(SliverShapes.R12).clickable { onEditLayer(i) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            val playing = preview.playing == layer.path
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (playing) SliverColors.TextPrimary else SliverColors.Surface3)
                    .clickable(role = Role.Button) { preview.toggle(layer.path) }
                    .semantics { contentDescription = if (playing) "Stop preview" else "Play preview" },
                Alignment.Center,
            ) {
                SliverIcon(if (playing) SliverIcons.Pause else SliverIcons.Play, if (playing) SliverColors.BgBase else SliverColors.TextPrimary, size = 16.dp)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(layerTint(i)))
                    SliverText(
                        trackTitle(layer.path),
                        SliverType.BodySmall,
                        color = if (layer.muted) SliverColors.TextTertiary else SliverColors.TextPrimary,
                        maxLines = 1
                    )
                }
                SliverText(layerSummary(layer, filmMs), SliverType.Meta, color = SliverColors.TextTertiary, maxLines = 1)
            }
            IconButton44(
                { menu = !menu },
                "More for ${trackTitle(layer.path)}"
            ) { SliverIcon(SliverIcons.MoreHorizontal, SliverColors.TextSecondary, size = 20.dp) }
        }
        if (menu) {
            Row(Modifier.padding(start = 52.dp, bottom = SliverSpace.S1), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                GhostButton(if (layer.muted) "Unmute" else "Mute", {
                    onMix(mix.copy(layers = mix.layers.toMutableList().also { it[i] = layer.copy(muted = !layer.muted) }))
                    menu = false
                })
                GhostButton("Remove", {
                    if (preview.playing == layer.path) preview.stop()
                    onMix(mix.copy(layers = mix.layers.filterIndexed { j, _ -> j != i }))
                    menu = false
                }, destructive = true)
            }
        }
    }
}

@Composable
private fun layerTint(i: Int): androidx.compose.ui.graphics.Color = SliverColors.LayerTints.let { it[i % it.size] }

/** "1:12 of 3:40 · from 0:05 · repeats · 80 %" */
fun layerSummary(layer: AudioLayer, filmMs: Long): String = buildList {
    add(if (layer.lengthMs < layer.durationMs) "${audioTime(layer.lengthMs)} of ${audioTime(layer.durationMs)}" else audioTime(layer.durationMs))
    add("from ${audioTime(layer.startMs)}")
    if (layer.loop && layer.startMs + layer.lengthMs < filmMs) add("repeats")
    if (layer.muted) add("muted") else add("${layer.volume} %")
}.joinToString(" · ")

/** v1.9 §1.1: filmstrip of the clips (tap = mute/unmute that clip), one labelled bar per layer (tap = edit), playhead. */
@Composable
private fun MixTimeline(mix: AudioMix, onMix: (AudioMix) -> Unit, clips: List<MixClip>, filmMs: Long, playheadMs: () -> Long, onEditLayer: (Int) -> Unit) {
    if (filmMs <= 0) return
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().clip(SliverShapes.R12)) {
        val w = maxWidth
        fun x(ms: Long) = w * (ms.coerceIn(0, filmMs).toFloat() / filmMs)
        val clipsW = w * (clips.size * FILM_FRAME_MS / filmMs.toFloat())
        val current by rememberUpdatedState(mix)
        Column(verticalArrangement = Arrangement.spacedBy(LaneGap)) {
            // Row 1: clips. Sampled thumbnails, muted clips darkened with a crossed speaker.
            Box(
                Modifier.width(clipsW).height(StripH).clip(RoundedCornerShape(8.dp)).background(SliverColors.Surface3)
                    .pointerInput(clips) {
                        detectTapGestures { p ->
                            val c = clips.getOrNull((p.x / size.width * clips.size).toInt().coerceIn(0, clips.size - 1)) ?: return@detectTapGestures
                            if (!c.keepAudio) return@detectTapGestures
                            val key = c.date.toString()
                            val m = current
                            onMix(m.copy(mutedClips = if (key in m.mutedClips) m.mutedClips - key else m.mutedClips + key))
                        }
                    }
                    .semantics { contentDescription = "Clips, tap one to mute it" },
            ) {
                val picks = remember(
                    clips
                ) {
                    if (clips.isEmpty()) emptyList() else List(minOf(20, clips.size)) { i -> clips[i * clips.size / minOf(20, clips.size)] }
                }
                Row(Modifier.fillMaxSize()) {
                    picks.forEach { c ->
                        val img = c.thumb?.let { app.sliver.feature.trim.rememberPhoto(it) }
                        Box(Modifier.weight(1f).fillMaxSize().background(c.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3)) {
                            if (img != null) {
                                androidx.compose.foundation.Image(
                                    img,
                                    null,
                                    Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                clips.forEachIndexed { i, c ->
                    val off = !c.keepAudio || c.date.toString() in mix.mutedClips || mix.clipVolume == 0
                    if (off) {
                        val cw = clipsW / clips.size
                        Box(Modifier.offset(x = cw * i).width(cw).fillMaxHeight().background(Color.Black.copy(alpha = 0.55f)), Alignment.Center) {
                            if (cw >= 16.dp) SliverIcon(SliverIcons.VolumeX, SliverColors.OnMedia, size = 12.dp)
                        }
                    }
                }
            }
            // One bar per layer, labelled inside, in its own tint.
            mix.layers.forEachIndexed { i, l ->
                val end = l.endMs(filmMs)
                Box(Modifier.fillMaxWidth().height(LaneH)) {
                    if (end > l.startMs) {
                        Box(
                            Modifier.offset(x = x(l.startMs)).width(x(end) - x(l.startMs)).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                                .background(layerTint(i).copy(alpha = if (l.muted) 0.25f else layerTint(i).alpha))
                                .clickable(role = Role.Button) { onEditLayer(i) }
                                .padding(horizontal = 8.dp),
                            Alignment.CenterStart,
                        ) {
                            SliverText(
                                trackTitle(l.path),
                                ReelogType.caption.copy(fontWeight = FontWeight.Medium),
                                color = SliverColors.OnMedia,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        // Playhead across all rows.
        val head = SliverColors.TextPrimary
        Canvas(Modifier.matchParentSize()) {
            val hx = (playheadMs().coerceIn(0, filmMs).toFloat() / filmMs) * size.width
            drawLine(head, Offset(hx, 0f), Offset(hx, size.height), 2.dp.toPx())
        }
    }
}

/** Edit one layer: where it starts in the film, which part of the file, volume, repeat, remove. */
@Composable
fun LayerEditor(layer: AudioLayer, filmMs: Long, playheadMs: () -> Long, onChange: (AudioLayer) -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(trackTitle(layer.path), SliverType.Title, maxLines = 1)
        SliverText("File length ${audioTime(layer.durationMs)}", SliverType.Meta, color = SliverColors.TextTertiary)

        ValueRow("Starts in the film at", audioTime(layer.startMs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SliverSlider(
                if (filmMs > 0) layer.startMs.toFloat() / filmMs else 0f,
                { onChange(layer.copy(startMs = (it * filmMs).toLong().roundTo(100))) },
                Modifier.weight(1f),
            )
            GhostButton("At playhead", { onChange(layer.copy(startMs = playheadMs().coerceIn(0, filmMs).roundTo(100))) })
        }

        val out = layer.outMs ?: layer.durationMs
        ValueRow("Trim", "${audioTime(layer.inMs)} – ${audioTime(out)}  (${audioTime(layer.lengthMs)})")
        TrimBar(
            layer.inMs,
            out,
            layer.durationMs,
            onChange = { a, b -> onChange(layer.copy(inMs = a, outMs = b.takeIf { it < layer.durationMs })) },
        )

        ValueRow("Volume", "${layer.volume}")
        SliverSlider(layer.volume / 100f, { onChange(layer.copy(volume = (it * 100).toInt().coerceIn(0, 100))) })

        SwitchRow(
            "Repeat to the end of the film",
            layer.loop,
            { onChange(layer.copy(loop = it)) },
            caption = "Plays the trimmed part again until the film ends.",
        )
        SwitchRow("Mute this layer", layer.muted, { onChange(layer.copy(muted = it)) })
        GhostButton("Remove layer", onRemove, destructive = true)
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SliverText(label, SliverType.BodySmall, Modifier.weight(1f), SliverColors.TextSecondary)
        SliverText(value, SliverType.Meta, color = SliverColors.TextSecondary)
    }
}

private const val MIN_TRIM_MS = 500L

/** Two handles over the file's length; drag either to set the part that plays. */
@Composable
private fun TrimBar(inMs: Long, outMs: Long, durationMs: Long, onChange: (Long, Long) -> Unit) {
    val a by rememberUpdatedState(inMs)
    val b by rememberUpdatedState(outMs)
    val change by rememberUpdatedState(onChange)
    var dragging by remember { mutableStateOf(0) }
    val track = SliverColors.Surface3
    val kept = SliverColors.TextTertiary
    val handle = SliverColors.TextPrimary
    Canvas(
        // Inset so the handles sit clear of the system back-gesture edges.
        Modifier.padding(
            horizontal = SliverSpace.S3
        ).fillMaxWidth().height(48.dp).semantics { contentDescription = "Trim ${audioTime(inMs)} to ${audioTime(outMs)}" }
            .pointerInput(durationMs) {
                if (durationMs <= 0) return@pointerInput
                fun ms(x: Float) = (x / size.width * durationMs).toLong().coerceIn(0, durationMs).roundTo(100)
                detectDragGestures(
                    onDragStart = { p ->
                        val t = ms(p.x)
                        dragging = if (kotlin.math.abs(t - a) <= kotlin.math.abs(t - b)) 1 else 2
                    },
                    onDragEnd = { dragging = 0 },
                    onDragCancel = { dragging = 0 },
                ) { c, _ ->
                    val t = ms(c.position.x)
                    if (dragging == 1) {
                        change(t.coerceAtMost(b - MIN_TRIM_MS).coerceAtLeast(0), b)
                    } else {
                        change(a, t.coerceAtLeast(a + MIN_TRIM_MS).coerceAtMost(durationMs))
                    }
                }
            },
    ) {
        if (durationMs <= 0) return@Canvas
        val h = 20.dp.toPx()
        val y = (size.height - h) / 2
        val r = CornerRadius(4.dp.toPx())
        fun x(ms: Long) = ms.toFloat() / durationMs * size.width
        drawRoundRect(track, Offset(0f, y), Size(size.width, h), r)
        drawRoundRect(kept, Offset(x(inMs), y), Size(x(outMs) - x(inMs), h), r)
        val hw = 4.dp.toPx()
        drawRoundRect(handle, Offset(x(inMs).coerceAtMost(size.width - hw), y - 6f), Size(hw, h + 12f), r)
        drawRoundRect(handle, Offset((x(outMs) - hw).coerceAtLeast(0f), y - 6f), Size(hw, h + 12f), r)
    }
}

/**
 * Audit v1.8 §5 music list (board phone 4): 64 dp rows with a play/pause preview, a cleaned title, how the track fits
 * the film, and a check when the track is on the film (an audio layer). The checked row opens its volume. Hold a row to
 * rename or remove it. Last row: Add music from files.
 */
@Composable
fun MusicList(
    files: List<String>,
    durationOf: (String) -> Long,
    filmMs: Long,
    mix: AudioMix,
    onMix: (AudioMix) -> Unit,
    notice: String?,
    onImport: () -> Unit,
    onDone: () -> Unit,
    onRemoved: () -> Unit,
) {
    val preview = rememberTrackPreview()
    val store = rememberMusicStore()
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<String?>(null) }
    Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
        SliverText("Music", SliverType.Title)
        SliverText("Film length ${audioTime(filmMs)}", SliverType.Meta, color = SliverColors.TextTertiary)
        if (files.isEmpty()) SliverText("No music yet", SliverType.BodySmall, Modifier.padding(vertical = SliverSpace.S2), SliverColors.TextSecondary)
        files.forEach { path ->
            val d = remember(path) { durationOf(path) }
            val index = mix.layers.indexOfFirst { it.path == path }
            val on = index >= 0
            fun toggle() {
                onMix(
                    if (on) {
                        mix.copy(layers = mix.layers.filter { it.path != path })
                    } else {
                        mix.copy(layers = mix.layers + AudioLayer(path, d))
                    },
                )
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (on) SliverColors.OnMedia.copy(alpha = 0.06f) else androidx.compose.ui.graphics.Color.Transparent),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp)
                        .combinedClickable(onLongClick = { menuFor = if (menuFor == path) null else path }, onClick = { if (!on) toggle() })
                        .padding(horizontal = SliverSpace.S2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
                ) {
                    val playing = preview.playing == path
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (playing) SliverColors.TextPrimary else SliverColors.Surface3)
                            .clickable(role = Role.Button) { preview.toggle(path) }
                            .semantics { contentDescription = if (playing) "Stop preview" else "Play preview" },
                        Alignment.Center,
                    ) {
                        SliverIcon(
                            if (playing) SliverIcons.Pause else SliverIcons.Play,
                            if (playing) SliverColors.BgBase else SliverColors.TextPrimary,
                            size = 16.dp
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        SliverText(trackTitle(path), SliverType.BodySmall, color = SliverColors.TextPrimary, maxLines = 1)
                        SliverText(fitCaption(d, filmMs), SliverType.Meta, color = SliverColors.TextTertiary, maxLines = 1)
                    }
                    // Trailing check (22 dp): on the film or not.
                    Box(
                        Modifier.size(44.dp).clickable(role = Role.Checkbox) { toggle() }
                            .semantics {
                                contentDescription = if (on) "Remove from film" else "Add to film"
                                selected = on
                            },
                        Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .then(
                                    if (on) Modifier.background(SliverColors.TextPrimary) else Modifier.border(1.5.dp, SliverColors.TextTertiary, CircleShape)
                                ),
                            Alignment.Center,
                        ) { if (on) SliverIcon(SliverIcons.Check, SliverColors.BgBase, size = 14.dp) }
                    }
                }
                if (on) {
                    val layer = mix.layers[index]
                    Row(
                        Modifier.fillMaxWidth().padding(start = 64.dp, end = SliverSpace.S3, bottom = SliverSpace.S1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
                    ) {
                        SliverText("Volume", SliverType.Meta, color = SliverColors.TextTertiary)
                        SliverSlider(
                            layer.volume / 100f,
                            { v ->
                                onMix(
                                    mix.copy(layers = mix.layers.toMutableList().also {
                                        it[index] = layer.copy(volume = (v * 100).toInt().coerceIn(0, 100))
                                    })
                                )
                            },
                            Modifier.weight(1f),
                        )
                        SliverText("${layer.volume} %", SliverType.Meta, color = SliverColors.TextTertiary)
                    }
                }
                if (menuFor == path) {
                    Row(Modifier.padding(start = 56.dp, bottom = SliverSpace.S1), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        GhostButton("Rename", {
                            renaming = path
                            menuFor = null
                        })
                        GhostButton("Remove", {
                            confirmRemove = path
                            menuFor = null
                        }, destructive = true)
                    }
                }
                if (renaming == path) {
                    var title by remember(path) { mutableStateOf(trackTitle(path)) }
                    Column(Modifier.padding(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        app.sliver.core.ui.SliverTextField(title, { title = it.take(60) }, placeholder = "Track name", autoFocus = true)
                        app.sliver.core.ui.PrimaryButton("Save", {
                            store.rename(path, title)
                            renaming = null
                        }, enabled = title.isNotBlank())
                    }
                }
                if (confirmRemove == path) {
                    Column(Modifier.padding(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
                        SliverText(
                            "Remove \u201C${trackTitle(path)}\u201D from the app? Films using it play without it.",
                            SliverType.BodySmall,
                            color = SliverColors.TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                            GhostButton("Remove", {
                                preview.stop()
                                onMix(mix.copy(layers = mix.layers.filter { it.path != path }))
                                store.remove(path)
                                confirmRemove = null
                                onRemoved()
                            }, destructive = true)
                            GhostButton("Cancel", { confirmRemove = null })
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(
                56.dp
            ).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onImport).padding(horizontal = SliverSpace.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            Box(
                Modifier.size(40.dp).border(1.dp, SliverColors.LineStrong, CircleShape),
                Alignment.Center
            ) { SliverIcon(SliverIcons.Plus, SliverColors.TextPrimary, size = 18.dp) }
            SliverText("Add music from files", SliverType.BodySmall, color = SliverColors.TextPrimary)
        }
        notice?.let { SliverText(it, SliverType.Caption, color = SliverColors.Error) }
        SliverText(
            "Names are cleaned from the file name. Hold a row to rename or remove.",
            SliverType.Caption,
            Modifier.padding(vertical = SliverSpace.S2),
            SliverColors.TextTertiary,
        )
        app.sliver.core.ui.PrimaryButton("Done", {
            preview.stop()
            onDone()
        }, Modifier.fillMaxWidth())
    }
}

private fun Long.roundTo(step: Long) = (this + step / 2) / step * step

private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun shortDate(d: LocalDate) = "${d.day} ${Months[d.month.ordinal]}"

/** v1.9 §1.3: every clip in a grid; tap to mute or unmute its own sound in this film. */
@Composable
fun MutedClipsGrid(mix: AudioMix, onMix: (AudioMix) -> Unit, clips: List<MixClip>, onDone: () -> Unit) {
    Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText("Muted clips", SliverType.Title)
        SliverText("Tap a clip to mute or unmute its own sound in this film.", SliverType.BodySmall, color = SliverColors.TextSecondary)
        clips.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { c ->
                    val key = c.date.toString()
                    val off = !c.keepAudio || key in mix.mutedClips
                    val img = c.thumb?.let { app.sliver.feature.trim.rememberPhoto(it) }
                    Box(
                        Modifier.weight(1f).aspectRatioSquare().clip(RoundedCornerShape(8.dp))
                            .background(c.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3)
                            .clickable(enabled = c.keepAudio, role = Role.Switch) {
                                onMix(mix.copy(mutedClips = if (key in mix.mutedClips) mix.mutedClips - key else mix.mutedClips + key))
                            }
                            .semantics { contentDescription = "${shortDate(c.date)} sound ${if (off) "muted" else "on"}" },
                    ) {
                        if (img != null) {
                            androidx.compose.foundation.Image(
                                img,
                                null,
                                Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        if (off) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                        Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp).clip(CircleShape).background(SliverColors.Scrim), Alignment.Center) {
                            SliverIcon(if (off) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.OnMedia, size = 11.dp)
                        }
                        SliverText(
                            shortDate(c.date),
                            ReelogType.caption.copy(fontWeight = FontWeight.Medium),
                            Modifier.align(Alignment.BottomStart).padding(4.dp),
                            color = SliverColors.OnMedia,
                            maxLines = 1
                        )
                    }
                }
                repeat(5 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        app.sliver.core.ui.PrimaryButton("Done", onDone, Modifier.fillMaxWidth().padding(top = SliverSpace.S2))
    }
}

private fun Modifier.aspectRatioSquare() = this.aspectRatio(1f)

private val LaneH = 28.dp
private val LaneGap = 6.dp
private val StripH = 36.dp
