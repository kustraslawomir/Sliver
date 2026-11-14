package app.sliver

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.Modifier

import app.sliver.core.ui.NoticeHost
import app.sliver.core.ui.UndoToastHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import app.sliver.data.settings.Appearance
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.sliver.core.navigation.SliverNavHost
import app.sliver.core.theme.SliverTheme
import app.sliver.core.theme.rememberReducedMotion
import app.sliver.core.ui.SplashOverlay
import app.sliver.debug.ComponentGallery

/** Set true to show the debug ComponentGallery instead of the app. */
const val SHOW_GALLERY = false

@Composable
fun App() {
    val repo = remember { KoinPlatform.getKoin().get<SettingsRepository>() }
    val appearance by remember { repo.settings.map { it.appearance } }.collectAsStateWithLifecycle(Appearance.SYSTEM)
    val light = when (appearance) {
        Appearance.SYSTEM -> !isSystemInDarkTheme()
        Appearance.LIGHT -> true
        Appearance.DARK -> false
    }
    SliverTheme(reducedMotion = rememberReducedMotion(), light = light) {
        if (SHOW_GALLERY) ComponentGallery()
        else {
            var splash by rememberSaveable { mutableStateOf(true) }
            val toProject by remember { repo.settings.map<_, Boolean?> { it.onboardingDone } }.collectAsStateWithLifecycle(null)
            val glass = dev.chrisbanes.haze.rememberHazeState()
            Box {
                androidx.compose.runtime.CompositionLocalProvider(app.sliver.core.kit.LocalGlassState provides glass) {
                    Box(Modifier.hazeSource(glass)) { SliverNavHost() }
                }
                NoticeHost()
                androidx.compose.runtime.CompositionLocalProvider(app.sliver.core.kit.LocalGlassState provides glass) { UndoToastHost() }
                if (splash) SplashOverlay(toProject) { splash = false }
            }
        }
    }
}
