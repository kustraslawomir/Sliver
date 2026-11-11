package app.sliver

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.sliver.core.navigation.SliverNavHost
import app.sliver.core.theme.SliverTheme
import app.sliver.core.theme.rememberReducedMotion
import app.sliver.core.ui.ClipDropOverlay
import app.sliver.core.ui.SplashOverlay
import app.sliver.debug.ComponentGallery

/** Set true to show the debug ComponentGallery instead of the app. */
const val SHOW_GALLERY = false

@Composable
fun App() {
    SliverTheme(reducedMotion = rememberReducedMotion()) {
        if (SHOW_GALLERY) ComponentGallery()
        else {
            var splash by remember { mutableStateOf(true) }
            Box {
                SliverNavHost()
                ClipDropOverlay()
                if (splash) SplashOverlay { splash = false }
            }
        }
    }
}
