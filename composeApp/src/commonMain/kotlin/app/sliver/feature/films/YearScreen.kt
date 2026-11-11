package app.sliver.feature.films

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Badge
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

@Composable
fun YearInSecondsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val renderer = remember { koin.get<FilmRenderer>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val today = remember { todayLocal() }
    val year = today.year
    val days by remember { koin.get<DayRepository>().observeAll() }.collectAsState(emptyList())
    val list by remember { films.observeAll() }.collectAsState(emptyList())
    val stats = remember(days, list) { yearStats(year, days, list) }
    val unlocked = teaserState(today) == TeaserState.UNLOCKED
    val scope = rememberCoroutineScope()

    suspend fun yearFilmOrCreate() = findYearFilm(year, films.observeAll().first()) ?: yearFilm(
        year, settings.settings.first().newFilmFormat, live = false,
    ).also { films.save(it) }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgVoid).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(bottom = SliverSpace.S8),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3), verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
            if (!unlocked) Badge("Preview", Modifier.padding(start = SliverSpace.S2))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            Box(Modifier.fillMaxWidth(0.6f).height(2.dp).background(SliverColors.AccentCore))
            SliverText("$year", SliverType.DisplayL)
            SliverText("in Seconds", SliverType.Headline, color = SliverColors.TextSecondary)
        }
        Column(
            Modifier.padding(horizontal = SliverSpace.Gutter),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            Stat("Clips kept", "${stats.seconds}")
            Stat("Films made", "${stats.films}")
            Stat(if (stats.topMonths.size > 1) "Most filmed months" else "Most filmed month", monthsText(stats.topMonths))
            Stat("Favourite filter", stats.favouriteLook?.let(::lookName) ?: "None yet")
        }
        Column(
            Modifier.padding(horizontal = SliverSpace.Gutter),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            if (unlocked) {
                PrimaryButton("Watch the premiere", {
                    scope.launch {
                        val film = yearFilmOrCreate()
                        FilmNav.id = film.id
                        if (film.isStale(days)) {
                            renderer.start(film.id)
                            onNavigate(Route.Export)
                        } else {
                            onNavigate(Route.Premiere)
                        }
                    }
                }, Modifier.fillMaxWidth())
            } else {
                SliverText("The premiere opens on 26 December.", SliverType.Caption, color = SliverColors.TextTertiary)
            }
            SecondaryButton("Change music or filter", {
                scope.launch {
                    FilmNav.id = yearFilmOrCreate().id
                    onNavigate(Route.FilmBuilder)
                }
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
        SliverText(label, SliverType.Caption, color = SliverColors.TextTertiary)
        SliverText(value, SliverType.DisplayM)
    }
}
