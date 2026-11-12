package app.sliver.feature.films

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun FilmsSection(onNavigate: (Route) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val today = remember { todayLocal() }
    val list by remember { films.observeAll() }.collectAsState(emptyList())
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)

    LaunchedEffect(all.isNotEmpty(), app) {
        val first = all.minOfOrNull { it.date }
        val format = app?.newFilmFormat
        if (first != null && format != null) films.ensureDefaultFilm(first, format)
    }

    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter), verticalAlignment = Alignment.CenterVertically) {
            SliverText("Films", SliverType.Title.copy(letterSpacing = (-0.02).em, lineHeight = 22.sp), Modifier.weight(1f))
            val tap = rememberTapGate { onNavigate(Route.FilmBuilder) }
            Row(
                Modifier.height(36.dp).clip(SliverShapes.Pill).border(1.dp, SliverColors.LineStrong, SliverShapes.Pill)
                    .clickable(role = Role.Button, onClick = tap).padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SliverIcon(HubIcons.Plus, SliverColors.TextPrimary, size = 16.dp)
                SliverText("Make film", SliverType.Headline.copy(fontSize = 14.sp, lineHeight = 18.sp), maxLines = 1, softWrap = false)
            }
        }
        if (list.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = SliverSpace.Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(list, key = { _, f -> f.id }) { i, film ->
                    Poster(film, film.plan(all), SliverColors.PosterTints[i % SliverColors.PosterTints.size]) {
                        FilmNav.id = film.id
                        onNavigate(if (film.renderPath != null) Route.FilmPlayer else Route.Export)
                    }
                }
            }
        }
    }
}


@Composable
private fun Poster(film: Film, plan: FilmPlan, tint: Color, onClick: () -> Unit) {
    val renderer = remember { KoinPlatform.getKoin().get<FilmRenderer>() }
    val state by remember(film.id) { renderer.state(film.id) }.collectAsState(RenderState.Idle)
    Column(
        Modifier.width(132.dp).clickable(role = Role.Button, onClick = rememberTapGate(onClick))
            .semantics { contentDescription = "${film.name}, ${formatRuntime(plan.clips)}" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(198.dp).clip(SliverShapes.R14)
                .background(Brush.verticalGradient(0f to tint, 0.75f to SliverColors.BgVoid, 1f to SliverColors.BgVoid))
                .border(1.dp, SliverColors.LineHairline, SliverShapes.R14)
                .padding(12.dp),
        ) {
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SliverText("A film by you", mono(9.sp, 0.sp), color = SliverColors.OnMedia.copy(alpha = 0.7f))
                SliverText(film.name, SliverType.Title.copy(fontSize = 21.sp, lineHeight = 22.sp, letterSpacing = (-0.02).em), color = SliverColors.OnMedia)
            }
            when (val s = state) {
                is RenderState.Running -> Badge("Rendering ${s.percent}%", Modifier.align(Alignment.TopStart), SliverColors.Accent)
                RenderState.Failed -> Badge("Try again", Modifier.align(Alignment.TopStart).clickable { renderer.start(film.id) }, SliverColors.Accent)
                RenderState.Idle -> Unit
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SliverText(formatRuntime(plan.clips), mono(10.sp, 0.sp), color = SliverColors.TextPrimary)
            SliverText(if (film.live) "Live" else film.rangeText(), mono(10.sp, 0.sp), color = SliverColors.TextTertiary)
        }
    }
}

/** Geist (tabular figures) at regular weight, as the Project boards use it. */
fun mono(size: TextUnit, tracking: TextUnit, color: Color = SliverColors.TextPrimary) =
    SliverType.Meta.copy(color = color, fontSize = size, letterSpacing = tracking, fontWeight = FontWeight.Normal, lineHeight = TextUnit.Unspecified)
