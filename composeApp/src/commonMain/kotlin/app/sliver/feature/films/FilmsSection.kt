package app.sliver.feature.films

import androidx.compose.foundation.layout.size
import app.sliver.core.icons.SliverIcons
import app.sliver.data.pickerLabel
import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton

import app.sliver.core.theme.ReelogType

import kotlinx.coroutines.flow.map
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.HubIcons
import app.sliver.core.icons.SliverIcon
import app.sliver.core.ui.rememberTapGate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.Notices
import app.sliver.core.ui.SliverBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Badge
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.trim.rememberPhoto
import org.koin.mp.KoinPlatform

/** Films of the active project, shown under the calendar on the Project screen. */
@Composable
fun FilmsSection(onNavigate: (Route) -> Unit, onLongPress: (Film) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    // Owner: newest film on the left.
    val list by remember { films.observeAll().map { l -> l.sortedByDescending { it.renderedAt?.toEpochMilliseconds() ?: it.updatedAt } } }.collectAsState(emptyList())
    val all by remember { days.observeAll() }.collectAsState(emptyList())

    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter), verticalAlignment = Alignment.CenterVertically) {
            // HubV18: "Films 3" + small filled Make film.
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SliverText("Films", SliverType.Title.copy(fontSize = ReelogType.Size.headline, lineHeight = ReelogType.lineFor(ReelogType.Size.headline)))
                if (list.isNotEmpty()) SliverText("${list.size}", SliverType.tnum(), Modifier.padding(bottom = 3.dp), color = SliverColors.TextTertiary)
            }
            ReelogButton("Make film", { onNavigate(Route.FilmBuilder) }, kind = ButtonKind.Secondary, small = true, fullWidth = false, leading = HubIcons.Plus)
        }
        val versions = remember(list) { filmVersions(list) }
        if (list.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = SliverSpace.Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(list, key = { _, f -> f.id }) { i, film ->
                    Poster(
                        film, film.plan(all), SliverColors.PosterTints[i % SliverColors.PosterTints.size], versions[film.id],
                        onLongClick = { onLongPress(film) },
                    ) {
                        FilmNav.id = film.id
                        onNavigate(if (film.renderPath != null) Route.FilmPlayer else Route.Export)
                    }
                }
            }
        }
    }
}


@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Poster(film: Film, plan: FilmPlan, tint: Color, version: Int?, onLongClick: () -> Unit, onClick: () -> Unit) {
    val renderer = remember { KoinPlatform.getKoin().get<FilmRenderer>() }
    val state by remember(film.id) { renderer.state(film.id) }.collectAsState(RenderState.Idle)
    Column(
        Modifier.width(PosterW).combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = rememberTapGate(onClick))
            .semantics { contentDescription = "${film.name}, ${formatRuntime(plan.clips)}" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(PosterH).clip(PosterShape)
                .background(Brush.verticalGradient(0f to tint, 0.75f to SliverColors.BgVoid, 1f to SliverColors.BgVoid))
        ) {
            // v1.8: each film its own cover (10 % in; versions step further in).
            val poster = plan.days.getOrNull(coverIndex(plan.days.size, version))?.let { rememberPhoto(it.thumb) }
            if (poster != null) {
                Image(poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to SliverColors.Scrim)))
            }
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                SliverText(
                    film.name, ReelogType.poster, Modifier.align(Alignment.BottomStart),
                    color = SliverColors.OnMedia, maxLines = 2,
                )
                if (film.renderPath != null && state == RenderState.Idle) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(30.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SliverColors.Scrim),
                        Alignment.Center,
                    ) { SliverIcon(SliverIcons.Play, SliverColors.OnMedia, size = 14.dp) }
                }
                when (val s = state) {
                    is RenderState.Running -> Badge("Rendering ${s.percent}%", Modifier.align(Alignment.TopStart), SliverColors.Accent)
                    RenderState.Failed -> Badge("Try again", Modifier.align(Alignment.TopStart).clickable { renderer.start(film.id) }, SliverColors.Accent)
                    RenderState.Idle -> Unit
                }
            }
        }
        // HubV18: 13 sp, two lines — range / length · version · look.
        Column {
            SliverText(plan.spanText(), ReelogType.caption, color = SliverColors.TextSecondary, maxLines = 1)
            SliverText(posterCaption(plan.clips, version, film.look?.pickerLabel), ReelogType.caption.copy(fontFeatureSettings = "tnum"), color = SliverColors.TextSecondary, maxLines = 1)
        }
    }
}

/** Sheet for a long-pressed poster; lives at the screen root so it covers the whole window. */
@Composable
fun FilmActionsSheet(film: Film?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirm by remember(film) { mutableStateOf(false) }
    SliverBottomSheet(film != null && !confirm, onDismiss) {
        Column {
            SliverText(film?.name.orEmpty(), SliverType.Headline, Modifier.padding(bottom = SliverSpace.S2))
            GhostButton("Delete film\u2026", { confirm = true }, Modifier.fillMaxWidth(), destructive = true)
        }
    }
    // Owner 14 Nov: deleting is never a single tap.
    app.sliver.core.ui.ConfirmSheet(
        film != null && confirm, "Delete this film?", "The days stay in the project. The copy in your gallery stays too.", "Delete film",
        {
            if (film != null) scope.launch {
                runCatching { KoinPlatform.getKoin().get<FilmDeleter>().delete(film) }
                    .onFailure { Notices.show("Couldn't delete this film. Try again.") }
            }
        },
        onDismiss,
    )
}


private val PosterW = 112.dp
private val PosterH = 142.dp
private val PosterShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
