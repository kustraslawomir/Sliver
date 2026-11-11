package app.sliver.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.notifications.DeepLink
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.NotificationsScreen
import app.sliver.core.ui.UndoToastHost
import app.sliver.feature.archive.ArchiveScreen
import app.sliver.feature.archive.DayDetailScreen
import app.sliver.feature.backfill.BackfillScreen
import app.sliver.feature.backup.BackupScreen
import app.sliver.feature.camera.CameraScreen
import app.sliver.feature.films.ExportScreen
import app.sliver.feature.films.FilmBuilderScreen
import app.sliver.feature.films.FilmNav
import app.sliver.feature.films.FilmPlayerScreen
import app.sliver.feature.films.PremiereScreen
import app.sliver.feature.films.YearInSecondsScreen
import app.sliver.feature.films.FilmsScreen
import app.sliver.feature.gallery.GalleryScreen
import app.sliver.feature.looks.LooksScreen
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.trim.PhotoTrimScreen
import app.sliver.feature.trim.TrimScreen
import app.sliver.feature.trim.UNDO_WINDOW_MS
import androidx.compose.runtime.produceState
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.onboarding.OnboardingScreen
import app.sliver.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatform

@Composable
fun SliverNavHost() {
    val nav = rememberNavController()
    val undo = remember { KoinPlatform.getKoin().get<SaveUndo>() }
    val entry by nav.currentBackStackEntryAsState()
    val current = Route.fromPath(entry?.destination?.route)

    fun go(route: Route) {
        if (route.tabBar || route == Route.Camera) {
            nav.navigate(route.path) {
                popUpTo(nav.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            nav.navigate(route.path) { launchSingleTop = true }
        }
    }

    val link by DeepLink.target.collectAsState()
    LaunchedEffect(link) {
        when (val t = link ?: return@LaunchedEffect) {
            DeepLinkTarget.Camera -> go(Route.Camera)
            DeepLinkTarget.Backup -> go(Route.Backup)
            is DeepLinkTarget.Film -> {
                FilmNav.id = t.id
                go(Route.FilmPlayer)
            }
            is DeepLinkTarget.Day -> {
                ArchiveSession.selected = t.date
                go(Route.DayDetail)
            }
        }
        DeepLink.target.value = null
    }

    val onboardingDone by produceState<Boolean?>(null) {
        value = KoinPlatform.getKoin().get<SettingsRepository>().settings.first().onboardingDone
    }
    val done = onboardingDone ?: return Box(Modifier.fillMaxSize().background(SliverColors.BgBase))

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        NavHost(nav, startDestination = Route.start(done).path) {
            Route.entries.forEach { route ->
                composable(route.path) {
                    when (route) {
                        Route.Camera -> CameraScreen(::go)
                        Route.Trim -> TrimScreen(::go)
                        Route.PhotoTrim -> PhotoTrimScreen(::go)
                        Route.GalleryPicker -> GalleryScreen(::go)
                        Route.Looks -> LooksScreen(onBack = { nav.popBackStack() })
                        Route.Archive -> ArchiveScreen(::go)
                        Route.DayDetail -> DayDetailScreen(::go, onBack = { nav.popBackStack() })
                        Route.FillThePast -> BackfillScreen(onBack = { nav.popBackStack() })
                        Route.Films -> FilmsScreen(::go)
                        Route.FilmBuilder -> FilmBuilderScreen(
                            onBack = { nav.popBackStack() },
                            onSaved = { id ->
                                FilmNav.id = id
                                nav.navigate(Route.Export.path) { popUpTo(Route.FilmBuilder.path) { inclusive = true } }
                            },
                        )
                        Route.Export -> ExportScreen(
                            onDone = { nav.navigate(Route.Premiere.path) { popUpTo(Route.Export.path) { inclusive = true } } },
                            onBack = { nav.popBackStack() },
                        )
                        Route.Premiere -> PremiereScreen(
                            onDone = { nav.navigate(Route.FilmPlayer.path) { popUpTo(Route.Premiere.path) { inclusive = true } } },
                        )
                        Route.FilmPlayer -> FilmPlayerScreen(
                            onBack = { nav.popBackStack() },
                            onEdit = { go(Route.FilmBuilder) },
                        )
                        Route.YearInSeconds -> YearInSecondsScreen(::go, onBack = { nav.popBackStack() })
                        Route.Settings -> SettingsScreen(::go)
                        Route.Backup -> BackupScreen(onBack = { nav.popBackStack() })
                        Route.Notifications ->NotificationsScreen(onBack = { nav.popBackStack() })
                        Route.Onboarding -> OnboardingScreen(onFinished = {
                            nav.navigate(Route.Camera.path) { popUpTo(Route.Onboarding.path) { inclusive = true } }
                        })
                        else -> PlaceholderScreen(route, ::go)
                    }
                }
            }
        }
        UndoToastHost(
            undo.toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = SliverSpace.S4),
            durationMs = UNDO_WINDOW_MS,
        )
        if (current?.tabBar == true) {
            SliverTabBar(current, ::go, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun PlaceholderScreen(route: Route, onGo: (Route) -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S6)
            .padding(bottom = SliverSpace.S10 + SliverSpace.S8),
    ) {
        SliverText(route.title, SliverType.DisplayM)
        Column(Modifier.padding(top = SliverSpace.S6)) {
            Route.entries.filter { it != route }.forEach { target ->
                GhostButton(target.title, onClick = { onGo(target) })
            }
        }
    }
}
