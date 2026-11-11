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
import androidx.compose.foundation.layout.statusBarsPadding
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

@Composable
fun FilmsScreen(onNavigate: (Route) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val today = remember { todayLocal() }
    val list by remember { films.observeAll() }.collectAsState(emptyList())
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)
    val teaser = remember { teaserState(today) }

    LaunchedEffect(all.isNotEmpty(), app) {
        val first = all.minOfOrNull { it.date }
        val format = app?.newFilmFormat
        if (first != null && format != null) films.ensureDefaultFilm(first, format)
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4)
                .padding(bottom = SliverSpace.S10 + SliverSpace.S8),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
        ) {
            SliverText("Films", SliverType.DisplayM)
            if (teaser != TeaserState.HIDDEN) {
                YearTeaser(today.year, teaser == TeaserState.UNLOCKED) { onNavigate(Route.YearInSeconds) }
            }
            list.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    row.forEach { film ->
                        Poster(film, film.plan(all), Modifier.weight(1f)) { onNavigate(Route.FilmPlayer) }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            SecondaryButton("+ New film", { onNavigate(Route.FilmBuilder) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun YearTeaser(year: Int, unlocked: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(SliverShapes.L).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.AccentLine, SliverShapes.L)
            .then(if (unlocked) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S1),
    ) {
        Badge(if (unlocked) "Ready" else "Unlocks 26 Dec")
        SliverText("Your $year in Seconds", SliverType.DisplayM)
        SliverText(
            if (unlocked) "Your year, one second at a time." else "Keep going. It opens on 26 December.",
            SliverType.BodySmall,
        )
    }
}

@Composable
private fun Poster(film: Film, plan: FilmPlan, modifier: Modifier, onClick: () -> Unit) {
    val ratio = if (film.format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f
    val cover = plan.days.firstOrNull()
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(ratio).clip(SliverShapes.M).background(SliverColors.Surface2)
                .border(1.dp, SliverColors.LineHairline, SliverShapes.M),
        ) {
            cover?.let { PosterCover(it) }
            if (film.live) Badge("Live", Modifier.align(Alignment.TopStart).padding(SliverSpace.S2))
        }
        SliverText(film.name, SliverType.Headline)
        SliverText(
            "${if (film.format == FilmFormat.CINEMA) "16:9" else "9:16"} · ${formatRuntime(plan.seconds)}",
            SliverType.MonoS,
        )
        SliverText("${film.rangeText()} · ${plan.seconds} seconds", SliverType.Caption)
    }
}

@Composable
private fun PosterCover(day: Day) {
    val thumb = rememberPhoto(day.thumb)
    if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
