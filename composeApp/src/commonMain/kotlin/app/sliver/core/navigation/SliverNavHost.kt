package app.sliver.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverSpace
import app.sliver.data.DaySource
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.archive.ProjectScreen
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.archive.DayDetailScreen
import app.sliver.feature.backfill.BackfillScreen
import app.sliver.feature.backup.BackupScreen
import app.sliver.feature.backup.RestoreScreen
import app.sliver.feature.camera.CameraScreen
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.films.ExportScreen
import app.sliver.feature.films.FilmBuilderScreen
import app.sliver.feature.films.FilmNav
import app.sliver.feature.films.FilmPlayerScreen
import app.sliver.feature.films.PremiereScreen
import app.sliver.feature.films.YearInSecondsScreen
import app.sliver.feature.gallery.GalleryScreen
import app.sliver.feature.looks.LooksScreen
import app.sliver.feature.notifications.DeepLink
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.NotificationsScreen
import app.sliver.feature.onboarding.OnboardingScreen
import app.sliver.feature.settings.SettingsScreen
import app.sliver.feature.trim.PhotoTrimScreen
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.trim.TrimScreen
import app.sliver.feature.trim.UNDO_WINDOW_MS
import app.sliver.core.ui.UndoToastHost
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

@Composable
fun SliverNavHost() {
    val nav = rememberNavController()
    val navigator = remember(nav) { AppNavigator(nav) }
    val undo = remember { KoinPlatform.getKoin().get<SaveUndo>() }
    val go = navigator::go

    val link by DeepLink.target.collectAsState()
    LaunchedEffect(link) {
        navigator.openStack(link ?: return@LaunchedEffect)
        DeepLink.target.value = null
    }

    val onboardingDone by produceState<Boolean?>(null) {
        value = KoinPlatform.getKoin().get<SettingsRepository>().settings.first().onboardingDone
    }
    val done = onboardingDone ?: return Box(Modifier.fillMaxSize().background(SliverColors.BgBase))

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
            NavHost(
                nav, startDestination = Route.start(done).path,
                enterTransition = { if (isVertical()) slideInVertically(SliverMotion.emphasized(SliverMotion.Base)) { it } else slideInHorizontally(SliverMotion.emphasized(SliverMotion.Base)) { it } },
                exitTransition = { if (isVertical()) fadeOut(SliverMotion.exit(SliverMotion.Base)) else slideOutHorizontally(SliverMotion.emphasized(SliverMotion.Base)) { -it / 4 } },
                popEnterTransition = { if (isVertical()) fadeIn(SliverMotion.standard(SliverMotion.Base)) else slideInHorizontally(SliverMotion.emphasized(SliverMotion.Base)) { -it / 4 } },
                popExitTransition = { if (isVertical()) slideOutVertically(SliverMotion.exit(SliverMotion.Base)) { it } else slideOutHorizontally(SliverMotion.exit(SliverMotion.Base)) { it } },
            ) {
                Route.entries.forEach { route ->
                    composable(route.path) { entry ->
                        ConsumeSavedClip(entry, route, undo, navigator)
                        val back = navigator::back
                        when (route) {
                            Route.Camera -> CameraScreen(go)
                            Route.Trim -> {
                                restoreCapture(entry.savedStateHandle)
                                TrimScreen(go, entry.savedStateHandle)
                            }
                            Route.PhotoTrim -> {
                                restoreCapture(entry.savedStateHandle)
                                PhotoTrimScreen(go)
                            }
                            Route.GalleryPicker -> GalleryScreen(go)
                            Route.Looks -> LooksScreen(onBack = back)
                            Route.Project -> ProjectScreen(go, onBack = back)
                            Route.DayDetail -> DayDetailScreen(go, onBack = back)
                            Route.FillThePast -> BackfillScreen(onBack = back)
                            Route.FilmBuilder -> FilmBuilderScreen(
                                onBack = back,
                                onSaved = { id ->
                                    FilmNav.id = id
                                    nav.navigate(Route.Export.path) { popUpTo(Route.FilmBuilder.path) { inclusive = true } }
                                },
                            )
                            Route.Export -> ExportScreen(
                                onDone = { nav.navigate(Route.Premiere.path) { popUpTo(Route.Export.path) { inclusive = true } } },
                                onBack = back,
                            )
                            Route.Premiere -> PremiereScreen(
                                onDone = { nav.navigate(Route.FilmPlayer.path) { popUpTo(Route.Premiere.path) { inclusive = true } } },
                            )
                            Route.FilmPlayer -> FilmPlayerScreen(onBack = back, onEdit = { go(Route.FilmBuilder) })
                            Route.YearInSeconds -> YearInSecondsScreen(go, onBack = back)
                            Route.Settings -> SettingsScreen(go, onBack = back)
                            Route.Backup -> BackupScreen(onBack = back, onRestore = { go(Route.Restore) })
                            Route.Restore -> RestoreScreen(onDone = { navigator.openStack(DeepLinkTarget.Camera) })
                            Route.Notifications -> NotificationsScreen(onBack = back)
                            Route.Onboarding -> OnboardingScreen(
                                onFinished = {
                                    nav.navigate(Route.Camera.path) { popUpTo(Route.Onboarding.path) { inclusive = true } }
                                },
                                onRestore = {
                                    nav.navigate(Route.Restore.path) { popUpTo(Route.Onboarding.path) { inclusive = true } }
                                },
                            )
                        }
                    }
                }
            }
            UndoToastHost(
                undo.toast,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = SliverSpace.S4),
                durationMs = UNDO_WINDOW_MS,
            )
        }
    }
}

/** Camera and Project are one vertical axis: Project slides up over Camera. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isVertical(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return setOf(from, to) == setOf(Route.Camera.path, Route.Project.path)
}

/** Shows the "Clip saved" toast on the screen a capture flow returned to. */
@Composable
private fun ConsumeSavedClip(entry: NavBackStackEntry, route: Route, undo: SaveUndo, navigator: AppNavigator) {
    val saved by entry.savedStateHandle.getStateFlow<String?>(SAVED_CLIP_KEY, null).collectAsState()
    LaunchedEffect(saved) {
        val date = saved?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@LaunchedEffect
        entry.savedStateHandle[SAVED_CLIP_KEY] = null
        undo.announceSaved(
            onView = if (route == Route.DayDetail) null else ({
                ArchiveSession.selected = date
                navigator.go(Route.DayDetail)
            }),
        )
    }
}

/** CaptureSession is in-memory; mirror it into the entry's SavedStateHandle so Edit clip reopens after process death. */
private fun restoreCapture(handle: SavedStateHandle) {
    val path = CaptureSession.path
    if (path != null) {
        handle[CAPTURE_PATH] = path
        handle[CAPTURE_SOURCE] = CaptureSession.source.name
        handle[CAPTURE_DATE] = CaptureSession.forDate?.toString()
        return
    }
    handle.get<String>(CAPTURE_PATH)?.let { saved ->
        CaptureSession.path = saved
        CaptureSession.source = handle.get<String>(CAPTURE_SOURCE)?.let(DaySource::valueOf) ?: DaySource.CAMERA
        CaptureSession.forDate = handle.get<String>(CAPTURE_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
}

private const val CAPTURE_PATH = "capture.path"
private const val CAPTURE_SOURCE = "capture.source"
private const val CAPTURE_DATE = "capture.date"

