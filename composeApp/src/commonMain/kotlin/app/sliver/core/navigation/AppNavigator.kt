package app.sliver.core.navigation

import app.sliver.core.ui.ClipDrop
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.films.FilmNav
import app.sliver.feature.notifications.DeepLinkTarget
import kotlinx.datetime.LocalDate

/** Screens that belong to a capture flow; the first screen below them is where the flow started. */
private val CaptureRoutes = setOf(Route.Camera, Route.Trim, Route.PhotoTrim)

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

    /** Swaps Edit clip for Photo clip (or back) in place; the same editor is rebuilt through the session revision. */
    fun swapEditor(target: Route) {
        val from = current ?: return
        if (from !in CaptureRoutes) return
        if (from == target) {
            CaptureSession.revision++
            return
        }
        nav.navigate(target.path) { popUpTo(from.path) { inclusive = true } }
    }

    fun back() {
        if (settled) nav.popBackStack()
    }

    private fun popToHome() {
        if (!nav.popBackStack(Route.Project.path, inclusive = false)) {
            nav.navigate(Route.Project.path) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    private fun popToCamera() {
        if (!nav.popBackStack(Route.Camera.path, inclusive = false)) {
            popToHome()
            push(Route.Camera)
        }
    }

    /** Save lands on the Project screen: capture screens are popped, Back from Project goes to Camera. Replace flows return to Clip detail. */
    fun finishFlow(date: LocalDate) {
        val stack = nav.currentBackStack.value.mapNotNull { route(it.destination.route) }
        val origin = stack.lastOrNull { it !in CaptureRoutes } ?: Route.Project
        ClipDrop.pending = date
        when (origin) {
            Route.Project, Route.DayDetail -> nav.popBackStack(origin.path, inclusive = false)
            else -> popToHome()
        }
    }

    /** Notification, widget and tile entries: Project (home) at the bottom, then the stack from NAVIGATION.md; record opens the camera modal. */
    fun openStack(target: DeepLinkTarget) {
        popToHome()
        when (target) {
            DeepLinkTarget.Camera -> push(Route.Camera)
            DeepLinkTarget.Backup -> listOf(Route.Settings, Route.Backup).forEach(::push)
            DeepLinkTarget.Project, DeepLinkTarget.Gallery -> Unit
            DeepLinkTarget.MakeFilm -> {
                FilmNav.editId = null
                push(Route.FilmBuilder)
            }
            DeepLinkTarget.FillThePast -> push(Route.FillThePast)
            is DeepLinkTarget.Film -> {
                FilmNav.id = target.id
                push(Route.FilmPlayer)
            }
            is DeepLinkTarget.Day -> {
                ArchiveSession.selected = target.date
                push(Route.DayDetail)
            }
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<AppNavigator> { error("AppNavigator not provided") }
