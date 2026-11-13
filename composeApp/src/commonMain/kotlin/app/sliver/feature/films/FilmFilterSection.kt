package app.sliver.feature.films

import app.sliver.data.pickerLabel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.SliverSlider
import app.sliver.data.FilterPresets
import app.sliver.data.LookPreset
import app.sliver.data.info

/** Renders [frame] once per filter at thumbnail size (cached by the platform); returns what is ready. */
expect suspend fun renderFilterThumbs(frame: ImageBitmap, presets: List<LookPreset>): Map<LookPreset, ImageBitmap>

private val TileW = 64.dp
private val TileH = 64.dp

/** Intensity and Grain for [selected], shown in the builder's adjust sheet. */
@Composable
fun FilterAmounts(selected: LookPreset, intensity: Int, grain: Int?, onIntensity: (Int) -> Unit, onGrain: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(selected.pickerLabel, SliverType.Title)
        AmountRow("Intensity", intensity, onIntensity)
        AmountRow("Grain", grain ?: (selected.info.model.grain * 100).toInt(), onGrain)
    }
}

/** The film's filter: a row of thumbnails rendered from a frame of the film, then Intensity and Grain for the chosen one. */
@Composable
fun FilmFilterSection(
    frame: ImageBitmap?,
    selected: LookPreset?,
    intensity: Int,
    grain: Int?,
    onSelect: (LookPreset?) -> Unit,
    onIntensity: (Int) -> Unit,
    onGrain: (Int) -> Unit,
    modifier: Modifier = Modifier,
    thumbsOverride: Map<LookPreset, ImageBitmap>? = null,
    /** Owner: sliders live behind a long press (or a tap on the chosen filter); null keeps them inline. */
    onAdjust: ((LookPreset) -> Unit)? = null,
) {
    val thumbs by produceState(thumbsOverride.orEmpty(), frame, thumbsOverride) {
        value = thumbsOverride ?: frame?.let { renderFilterThumbs(it, FilterPresets) }.orEmpty()
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(
            if (onAdjust != null) "Filter \u00B7 hold to adjust" else "Filter \u00B7 whole film",
            SliverType.BodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp), color = SliverColors.TextTertiary,
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            Tile("None", frame, selected == null, onClick = { onSelect(null) })
            FilterPresets.forEach { p ->
                Tile(
                    p.pickerLabel, thumbs[p], selected == p,
                    onClick = { if (selected == p && onAdjust != null) onAdjust(p) else onSelect(p) },
                    onLongClick = onAdjust?.let { adjust -> { onSelect(p); adjust(p) } },
                )
            }
        }
        if (selected != null && onAdjust == null) {
            val grainValue = grain ?: (selected.info.model.grain * 100).toInt()
            AmountRow("Intensity", intensity, onIntensity)
            AmountRow("Grain", grainValue, onGrain)
        }
    }
}

@Composable
private fun Tile(label: String, image: ImageBitmap?, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        Modifier.width(TileW).combinedClickable(role = Role.RadioButton, onLongClick = onLongClick, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = "$label filter" },
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S1),
    ) {
        Box(
            Modifier.size(TileW, TileH).clip(SliverShapes.S).background(SliverColors.Surface2)
                .then(if (selected) Modifier.border(2.dp, SliverColors.TextPrimary, SliverShapes.S) else Modifier),
        ) {
            if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        SliverText(
            label, SliverType.Caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center), Modifier.fillMaxWidth(),
            color = if (selected) SliverColors.TextPrimary else SliverColors.TextTertiary, maxLines = 1,
        )
    }
}

@Composable
private fun AmountRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SliverText(label, SliverType.BodySmall, Modifier.weight(1f), SliverColors.TextSecondary)
            SliverText("$value", SliverType.Meta, color = SliverColors.TextSecondary)
        }
        SliverSlider(value / 100f, { onChange((it * 100).toInt().coerceIn(0, 100)) })
    }
}
