package app.sliver.feature.films

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
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

class MixClip(val date: LocalDate, val keepAudio: Boolean)

private val LaneH = 14.dp
private val LaneGap = 6.dp

/**
 * Owner 14 Nov: sound like an editor. A timeline (clips lane + one lane per audio layer + playhead), the clips' own sound
 * (volume, mute single clips or all), and the audio layers (each opens [onEditLayer]).
 */
@Composable
fun AudioMixSection(
    mix: AudioMix,
    onMix: (AudioMix) -> Unit,
    clips: List<MixClip>,
    filmMs: Long,
    playheadMs: Long,
    onSeekMs: (Long) -> Unit,
    onAddLayer: () -> Unit,
    onEditLayer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText("Sound", SliverType.Title)
        MixTimeline(mix, clips, filmMs, playheadMs, onSeekMs)

        val clipsMuted = mix.clipVolume == 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            SliverText("Clip sound", SliverType.BodySmall, Modifier.weight(1f), SliverColors.TextSecondary)
            SliverText(if (clipsMuted) "Muted" else "${mix.clipVolume}", SliverType.Meta, color = SliverColors.TextSecondary)
            IconButton44(
                { onMix(mix.copy(clipVolume = if (clipsMuted) 100 else 0)) }, if (clipsMuted) "Unmute all clips" else "Mute all clips",
            ) { SliverIcon(if (clipsMuted) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.TextPrimary, size = 20.dp) }
        }
        SliverSlider(mix.clipVolume / 100f, { onMix(mix.copy(clipVolume = (it * 100).toInt().coerceIn(0, 100))) })
        if (!clipsMuted && clips.isNotEmpty()) {
            SliverText("Tap a clip to mute or unmute it", SliverType.Caption, color = SliverColors.TextTertiary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
                clips.forEach { c -> ClipChip(c, mix, onMix) }
            }
        }

        SliverText("Audio layers", SliverType.BodySmall, Modifier.padding(top = SliverSpace.S2), SliverColors.TextSecondary)
        if (mix.layers.isEmpty()) {
            SliverText("Add music or any audio file. Layers play together over the clips.", SliverType.Caption, color = SliverColors.TextTertiary)
        }
        mix.layers.forEachIndexed { i, layer -> LayerRow(i, layer, filmMs, mix, onMix, onEditLayer) }
        GhostButton("Add audio layer", onAddLayer)
    }
}

@Composable
private fun ClipChip(c: MixClip, mix: AudioMix, onMix: (AudioMix) -> Unit) {
    val key = c.date.toString()
    val on = c.keepAudio && key !in mix.mutedClips
    Row(
        Modifier.height(36.dp).clip(SliverShapes.Pill)
            .background(if (on) SliverColors.Surface2 else SliverColors.BgBase)
            .border(1.dp, if (on) SliverColors.LineHairline else SliverColors.LineStrong, SliverShapes.Pill)
            .clickable(enabled = c.keepAudio, role = Role.Switch) {
                onMix(mix.copy(mutedClips = if (on) mix.mutedClips + key else mix.mutedClips - key))
            }
            .semantics { contentDescription = "${shortDate(c.date)} sound ${if (on) "on" else "muted"}" }
            .padding(horizontal = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SliverIcon(if (on) SliverIcons.Volume2 else SliverIcons.VolumeX, if (on) SliverColors.TextPrimary else SliverColors.TextTertiary, size = 14.dp)
        SliverText(shortDate(c.date), SliverType.Meta, color = if (on) SliverColors.TextPrimary else SliverColors.TextTertiary)
    }
}

@Composable
private fun LayerRow(i: Int, layer: AudioLayer, filmMs: Long, mix: AudioMix, onMix: (AudioMix) -> Unit, onEditLayer: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(SliverShapes.R12).background(SliverColors.Surface2)
            .clickable { onEditLayer(i) }.padding(end = SliverSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton44(
            { onMix(mix.copy(layers = mix.layers.toMutableList().also { it[i] = layer.copy(muted = !layer.muted) })) },
            if (layer.muted) "Unmute layer" else "Mute layer",
        ) { SliverIcon(if (layer.muted) SliverIcons.VolumeX else SliverIcons.Volume2, if (layer.muted) SliverColors.TextTertiary else SliverColors.TextPrimary, size = 20.dp) }
        Column(Modifier.weight(1f)) {
            SliverText(
                trackLabel(layer.path) ?: "Audio", SliverType.BodySmall,
                color = if (layer.muted) SliverColors.TextTertiary else SliverColors.TextPrimary, maxLines = 1,
            )
            SliverText(layerSummary(layer, filmMs), SliverType.Meta, color = SliverColors.TextTertiary, maxLines = 1)
        }
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary, size = 18.dp)
    }
}

/** "1:12 of 3:40 · from 0:05 · repeats · 80 %" */
fun layerSummary(layer: AudioLayer, filmMs: Long): String = buildList {
    add(if (layer.lengthMs < layer.durationMs) "${audioTime(layer.lengthMs)} of ${audioTime(layer.durationMs)}" else audioTime(layer.durationMs))
    add("from ${audioTime(layer.startMs)}")
    if (layer.loop && layer.startMs + layer.lengthMs < filmMs) add("repeats")
    if (layer.muted) add("muted") else add("${layer.volume} %")
}.joinToString(" · ")

@Composable
private fun MixTimeline(mix: AudioMix, clips: List<MixClip>, filmMs: Long, playheadMs: Long, onSeekMs: (Long) -> Unit) {
    val lanes = 1 + mix.layers.size
    val seek by rememberUpdatedState(onSeekMs)
    val length by rememberUpdatedState(filmMs)
    val clipColor = SliverColors.Surface3
    val mutedColor = SliverColors.LineStrong
    val layerColor = SliverColors.TextTertiary
    val layerOff = SliverColors.Surface2
    val head = SliverColors.TextPrimary
    Canvas(
        Modifier.fillMaxWidth().height(LaneH * lanes + LaneGap * (lanes - 1) + 8.dp)
            .semantics { contentDescription = "Sound timeline" }
            .pointerInput(Unit) {
                detectTapGestures { seek((it.x / size.width * length).toLong()) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> seek((change.position.x / size.width * length).toLong().coerceIn(0, length)) }
            },
    ) {
        if (filmMs <= 0) return@Canvas
        val lane = LaneH.toPx()
        val gap = LaneGap.toPx()
        val r = CornerRadius(3.dp.toPx())
        fun x(ms: Long) = ms.toFloat() / filmMs * size.width
        val top = 4.dp.toPx()
        clips.forEachIndexed { i, c ->
            val x0 = x(i * FILM_FRAME_MS)
            val w = (x(FILM_FRAME_MS) - 1f).coerceAtLeast(1f)
            val on = c.keepAudio && c.date.toString() !in mix.mutedClips && mix.clipVolume > 0
            if (on) drawRoundRect(clipColor, Offset(x0, top), Size(w, lane), r)
            else drawRoundRect(mutedColor, Offset(x0, top + 1f), Size(w, lane - 2f), r, style = Stroke(1.dp.toPx()))
        }
        mix.layers.forEachIndexed { i, l ->
            val y = top + (i + 1) * (lane + gap)
            val end = l.endMs(filmMs)
            if (end <= l.startMs) return@forEachIndexed
            if (l.muted) drawRoundRect(layerColor, Offset(x(l.startMs), y + 1f), Size(x(end) - x(l.startMs), lane - 2f), r, style = Stroke(1.dp.toPx()))
            else drawRoundRect(layerColor, Offset(x(l.startMs), y), Size(x(end) - x(l.startMs), lane), r)
            // Repeats are marked where the file starts again.
            if (l.loop && l.lengthMs > 0) {
                var t = l.startMs + l.lengthMs
                while (t < end) { drawLine(layerOff, Offset(x(t), y), Offset(x(t), y + lane), 2f); t += l.lengthMs }
            }
        }
        val hx = x(playheadMs.coerceIn(0, filmMs))
        drawLine(head, Offset(hx, 0f), Offset(hx, size.height), 2.dp.toPx())
    }
}

/** Edit one layer: where it starts in the film, which part of the file, volume, repeat, remove. */
@Composable
fun LayerEditor(layer: AudioLayer, filmMs: Long, playheadMs: Long, onChange: (AudioLayer) -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(trackLabel(layer.path) ?: "Audio", SliverType.Title, maxLines = 1)
        SliverText("File length ${audioTime(layer.durationMs)}", SliverType.Meta, color = SliverColors.TextTertiary)

        ValueRow("Starts in the film at", audioTime(layer.startMs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SliverSlider(
                if (filmMs > 0) layer.startMs.toFloat() / filmMs else 0f,
                { onChange(layer.copy(startMs = (it * filmMs).toLong().roundTo(100))) }, Modifier.weight(1f),
            )
            GhostButton("At playhead", { onChange(layer.copy(startMs = playheadMs.coerceIn(0, filmMs).roundTo(100))) })
        }

        val out = layer.outMs ?: layer.durationMs
        ValueRow("Trim", "${audioTime(layer.inMs)} – ${audioTime(out)}  (${audioTime(layer.lengthMs)})")
        TrimBar(
            layer.inMs, out, layer.durationMs,
            onChange = { a, b -> onChange(layer.copy(inMs = a, outMs = b.takeIf { it < layer.durationMs })) },
        )

        ValueRow("Volume", "${layer.volume}")
        SliverSlider(layer.volume / 100f, { onChange(layer.copy(volume = (it * 100).toInt().coerceIn(0, 100))) })

        SwitchRow(
            "Repeat to the end of the film", layer.loop, { onChange(layer.copy(loop = it)) },
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
        Modifier.padding(horizontal = SliverSpace.S3).fillMaxWidth().height(48.dp).semantics { contentDescription = "Trim ${audioTime(inMs)} to ${audioTime(outMs)}" }
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
                    if (dragging == 1) change(t.coerceAtMost(b - MIN_TRIM_MS).coerceAtLeast(0), b)
                    else change(a, t.coerceAtLeast(a + MIN_TRIM_MS).coerceAtMost(durationMs))
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

/** Pick a file from the app's music library (with its length) or import a new one. */
@Composable
fun AudioLibrary(files: List<String>, durationOf: (String) -> Long, filmMs: Long, notice: String?, onPick: (String) -> Unit, onImport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
        SliverText("Add audio layer", SliverType.Title)
        SliverText("Film length ${audioTime(filmMs)}", SliverType.Meta, color = SliverColors.TextTertiary)
        files.forEach { path ->
            val d = remember(path) { durationOf(path) }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(SliverShapes.R12).clickable { onPick(path) }.padding(horizontal = SliverSpace.S2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
            ) {
                SliverIcon(SliverIcons.Volume2, SliverColors.TextTertiary, size = 18.dp)
                SliverText(trackLabel(path) ?: path.substringAfterLast('/'), SliverType.BodySmall, Modifier.weight(1f), SliverColors.TextPrimary, maxLines = 1)
                SliverText(if (d > 0) audioTime(d) else "–", SliverType.Meta, color = SliverColors.TextSecondary)
            }
        }
        if (files.isEmpty()) SliverText("No audio files yet.", SliverType.BodySmall, color = SliverColors.TextTertiary)
        GhostButton("Import an audio file…", onImport)
        notice?.let { SliverText(it, SliverType.Caption, color = SliverColors.Error) }
        Box(Modifier.size(1.dp))
    }
}

private fun Long.roundTo(step: Long) = (this + step / 2) / step * step

private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun shortDate(d: LocalDate) = "${d.day} ${Months[d.month.ordinal]}"
