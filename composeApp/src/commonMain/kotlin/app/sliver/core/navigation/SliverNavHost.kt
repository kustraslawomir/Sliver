package app.sliver.core.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import app.sliver.core.ui.LocalWindowSize
import app.sliver.core.ui.MaxContentWidth
import app.sliver.core.ui.WindowSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverSpace
import app.sliver.data.DaySource
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.archive.ProjectScreen
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.archive.DayDetailScreen
import app.sliver.feature.autoimport.AutoImportScreen
import app.sliver.feature.backup.BackupScreen
import app.sliver.feature.backup.RestoreScreen
import app.sliver.feature.camera.CameraScreen
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.films.ExportScreen
import app.sliver.feature.films.FilmBuilderScreen
import app.sliver.feature.films.FilmNav
import app.sliver.feature.films.FilmPlayerScreen
import app.sliver.feature.films.PremiereScreen
import app.sliver.feature.notifications.DeepLink
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.RemindersScreen
import app.sliver.feature.onboarding.OnboardingScreen
import app.sliver.feature.settings.SettingsScreen
import app.sliver.feature.trim.PhotoTrimScreen
import app.sliver.feature.trim.TrimScreen
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

@Composable
fun SliverNavHost() {
    val nav = rememberNavController()
    val navigator = remember(nav) { AppNavigator(nav) }
    val go = navigator::go
    val entry by nav.currentBackStackEntryAsState()
    val dark = Route.fromPath(entry?.destination?.route) in DarkRoutes
    // Set during composition, not in a SideEffect, so the incoming screen never paints one frame in the old palette.
    SliverColors.forceDark = dark

    val link by DeepLink.target.collectAsStateWithLifecycle()

    val onboardingDone by produceState<Boolean?>(null) {
        value = KoinPlatform.getKoin().get<SettingsRepository>().settings.first().onboardingDone
    }
    val done = onboardingDone ?: return Box(Modifier.fillMaxSize().background(SliverColors.BgBase))
    // The graph exists only once NavHost has composed; a cold-start link must wait for it. Before onboarding, links are dropped.
    LaunchedEffect(link) {
        val target = link ?: return@LaunchedEffect
        if (done) navigator.openStack(target)
        DeepLink.target.value = null
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
    CompositionLocalProvider(LocalNavigator provides navigator, LocalWindowSize provides WindowSize(maxWidth, maxHeight)) {
        Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
            NavHost(
                nav, startDestination = Route.start(done).path,
                enterTransition = { if (isEditorSwap()) EnterTransition.None else if (isVertical()) slideInVertically(SliverMotion.emphasized(SliverMotion.Base)) { it } else slideInHorizontally(SliverMotion.emphasized(SliverMotion.Base)) { it } },
                exitTransition = { if (isEditorSwap()) ExitTransition.None else if (isVertical()) fadeOut(SliverMotion.exit(SliverMotion.Base)) else slideOutHorizontally(SliverMotion.emphasized(SliverMotion.Base)) { -it / 4 } },
                popEnterTransition = { if (isVertical()) fadeIn(SliverMotion.standard(SliverMotion.Base)) else slideInHorizontally(SliverMotion.emphasized(SliverMotion.Base)) { -it / 4 } },
                popExitTransition = { if (isVertical()) slideOutVertically(SliverMotion.exit(SliverMotion.Base)) { it } else slideOutHorizontally(SliverMotion.exit(SliverMotion.Base)) { it } },
            ) {
                Route.entries.forEach { route ->
                    composable(route.path) { entry ->
                        val back = navigator::back
                        // The palette is global, so an outgoing screen whose palette differs from the target would repaint wrongly: hide it.
                        val wrongPalette = transition.targetState == EnterExitState.PostExit && (route in DarkRoutes) != dark
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (wrongPalette) 0f else 1f }) {
                        CappedWidth(route in CappedRoutes) {
                        when (route) {
                            Route.Camera -> CameraScreen(go, onClose = back)
                            Route.Trim -> {
                                restoreCapture(entry.savedStateHandle)
                                key(CaptureSession.revision) { TrimScreen(go, entry.savedStateHandle) }
                            }
                            Route.PhotoTrim -> {
                                restoreCapture(entry.savedStateHandle)
                                key(CaptureSession.revision) { PhotoTrimScreen(go) }
                            }
                            Route.Project -> ProjectScreen(go)
                            Route.DayDetail -> DayDetailScreen(go, onBack = back)
                            Route.FillThePast -> AutoImportScreen(go, onBack = back)
                            Route.DayGallery -> app.sliver.feature.gallery.DayGalleryScreen(go, onBack = back)
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
                            Route.FilmPlayer -> FilmPlayerScreen(onBack = back, onEdit = { FilmNav.editId = FilmNav.id; go(Route.FilmBuilder) })
                            Route.Settings -> SettingsScreen(go, onBack = back)
                            Route.Backup -> BackupScreen(onBack = back, onRestore = { go(Route.Restore) })
                            Route.Restore -> RestoreScreen(onDone = { navigator.openStack(DeepLinkTarget.Project) })
                            Route.Reminders -> RemindersScreen(onBack = back)
                            Route.Onboarding -> OnboardingScreen(
                                onFinished = {
                                    nav.navigate(Route.Project.path) { popUpTo(Route.Onboarding.path) { inclusive = true } }
                                },
                                onRestore = {
                                    nav.navigate(Route.Restore.path) { popUpTo(Route.Onboarding.path) { inclusive = true } }
                                },
                            )
                        }
                        }
                        }
                    }
                }
            }
        }
    }
    }
}

/** Media screens stay dark in both themes. */
private val DarkRoutes = setOf(Route.Camera, Route.Trim, Route.PhotoTrim, Route.Export, Route.Premiere, Route.FilmPlayer)

private val CappedRoutes = setOf(
    Route.FillThePast, Route.Settings, Route.Backup, Route.Restore, Route.Reminders,
    Route.FilmBuilder, Route.Export, Route.Premiere, Route.Onboarding,
)

/** Lists and forms stop growing at [MaxContentWidth] and sit centered on big windows. */
@Composable
private fun CappedWidth(capped: Boolean, content: @Composable () -> Unit) {
    if (!capped) return content()
    Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
        Box(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) { content() }
    }
}

private val EditorPaths = setOf(Route.Trim.path, Route.PhotoTrim.path)

/** Swapping a day's item between Edit clip and Photo clip happens in place: no slide (owner: "glitchy animations"). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isEditorSwap(): Boolean =
    initialState.destination.route in EditorPaths && targetState.destination.route in EditorPaths

/** Camera is a modal over Project: it slides up over home. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isVertical(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return setOf(from, to) == setOf(Route.Camera.path, Route.Project.path)
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

