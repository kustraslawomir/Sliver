package app.sliver.feature.autoimport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.sliver.feature.archive.MonthNames
import app.sliver.feature.camera.overMedia
import app.sliver.feature.trim.rememberPhoto
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.db.Day

/** The review row's sheet: drafts and auto-filled days waiting for a look (board ProjectHubReview). */
@Composable
fun ReviewSheetBody(
    drafts: Int, auto: Int, busy: Boolean, onKeepAll: () -> Unit, onRemoveUnchecked: () -> Unit, onWalk: () -> Unit,
    days: List<Day> = emptyList(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SliverText(reviewSheetTitle(drafts, auto), SliverType.Title)
        SliverText(reviewSheetBody(drafts, auto), SliverType.BodySmall, color = SliverColors.TextSecondary)
        if (days.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { DayThumb(it) }
            }
        }
        Spacer(Modifier.height(SliverSpace.S1))
        PrimaryButton("Keep all", onKeepAll, Modifier.fillMaxWidth(), enabled = !busy)
        SecondaryButton("Check one by one", onWalk, Modifier.fillMaxWidth())
        if (auto > 0) GhostButton("Remove unchecked", onRemoveUnchecked, Modifier.fillMaxWidth(), destructive = true, enabled = !busy)
    }
}

@Composable
private fun DayThumb(day: Day) {
    val thumb = rememberPhoto(day.thumb)
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(112.dp, 64.dp).clip(shape)
            .background(day.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3)
            .then(if (day.draft) Modifier.alpha(0.4f) else Modifier),
    ) {
        if (thumb != null) Image(thumb, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().dashedOutline(if (day.draft) SliverColors.TextDisabled else SliverColors.OnMedia))
        SliverText(
            "${day.date.day} ${MonthNames[day.date.month.ordinal]}", SliverType.Caption.copy(fontSize = 11.sp, color = SliverColors.OnMedia).overMedia(),
            Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 4.dp),
        )
    }
}

/** Dashed 1.5dp outline of auto-filled (white) and draft (grey) days. */
fun Modifier.dashedOutline(color: Color, radius: Dp = 8.dp): Modifier = drawBehind {
    val w = 1.5.dp.toPx()
    drawRoundRect(
        color, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), CornerRadius(radius.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}
