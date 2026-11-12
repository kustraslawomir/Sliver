package app.sliver

import app.sliver.core.ui.NoticeHost
import app.sliver.core.ui.UndoToastHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import app.sliver.data.settings.Appearance
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val appearance by remember { repo.settings.map { it.appearance } }.collectAsState(Appearance.SYSTEM)
    val light = when (appearance) {
        Appearance.SYSTEM -> !isSystemInDarkTheme()
        Appearance.LIGHT -> true
        Appearance.DARK -> false
    }
    SliverTheme(reducedMotion = rememberReducedMotion(), light = light) {
        if (SHOW_GALLERY) ComponentGallery()
        else {
            var splash by remember { mutableStateOf(true) }
            Box {
                SliverNavHost()
                NoticeHost()
                UndoToastHost()
                if (splash) SplashOverlay { splash = false }
            }
        }
    }
}
