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
import androidx.compose.runtime.remember
import app.sliver.core.ui.UndoToastHost
import app.sliver.feature.camera.CameraScreen
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.trim.TrimScreen
import app.sliver.feature.trim.UNDO_WINDOW_MS
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

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        NavHost(nav, startDestination = Route.start(OnboardingFlag.done).path) {
            Route.entries.forEach { route ->
                composable(route.path) {
                    when (route) {
                        Route.Camera -> CameraScreen(::go)
                        Route.Trim -> TrimScreen(::go)
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
            if (route == Route.Onboarding) {
                GhostButton("Finish onboarding", onClick = {
                    OnboardingFlag.done = true
                    onGo(Route.Camera)
                })
            }
            Route.entries.filter { it != route }.forEach { target ->
                GhostButton(target.title, onClick = { onGo(target) })
            }
        }
    }
}
