package app.sliver.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.films.FilmNav
import app.sliver.feature.notifications.DeepLinkTarget
import kotlinx.datetime.LocalDate

const val SAVED_CLIP_KEY = "clipSaved"

/** Screens that belong to a capture flow; the first screen below them is where the flow started. */
private val CaptureRoutes = setOf(Route.Camera, Route.GalleryPicker, Route.Trim, Route.PhotoTrim, Route.Looks)

/** Single entry point for navigation: de-duplicates events, returns capture flows to their origin, builds deep-link stacks. */
class AppNavigator(private val nav: NavController) {
    private var lastTarget: Route? = null

    private fun route(path: String?) = Route.fromPath(path)
    private val current get() = route(nav.currentBackStackEntry?.destination?.route)

    private val settled get() = nav.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

    private fun isDuplicate(target: Route): Boolean {
        if (!settled && lastTarget == target) return true
        lastTarget = target
        return false
    }

    private fun push(target: Route) = nav.navigate(target.path) { launchSingleTop = true }

    fun go(target: Route) {
        if (isDuplicate(target)) return
        val from = current
        when {
            target == Route.Camera && from == Route.Camera -> Unit
            target == Route.Camera && (from == Route.Project || from == Route.DayDetail) -> push(target)
            target == Route.Camera -> popToCamera()
            else -> push(target)
        }
    }

    fun back() {
        if (settled) nav.popBackStack()
    }

    private fun popToCamera() {
        if (!nav.popBackStack(Route.Camera.path, inclusive = false)) {
            nav.navigate(Route.Camera.path) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    /** Pops to the screen that opened the capture flow and hands it a one-shot "clip saved" result. */
    fun finishFlow(date: LocalDate) {
        val stack = nav.currentBackStack.value.mapNotNull { route(it.destination.route) }
        val origin = stack.lastOrNull { it !in CaptureRoutes } ?: Route.Camera
        runCatching { nav.getBackStackEntry(origin.path).savedStateHandle[SAVED_CLIP_KEY] = date.toString() }
        if (origin == Route.Camera) popToCamera() else nav.popBackStack(origin.path, inclusive = false)
    }

    /** Notification, widget and tile entries: always Camera at the bottom, then the stack from NAVIGATION.md. */
    fun openStack(target: DeepLinkTarget) {
        popToCamera()
        when (target) {
            DeepLinkTarget.Camera -> Unit
            DeepLinkTarget.Backup -> listOf(Route.Project, Route.Settings, Route.Backup).forEach(::push)
            is DeepLinkTarget.Film -> {
                FilmNav.id = target.id
                listOf(Route.Project, Route.FilmPlayer).forEach(::push)
            }
            is DeepLinkTarget.Day -> {
                ArchiveSession.selected = target.date
                listOf(Route.Project, Route.DayDetail).forEach(::push)
            }
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<AppNavigator> { error("AppNavigator not provided") }
