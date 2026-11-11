package app.sliver

import androidx.compose.runtime.Composable
import app.sliver.core.navigation.SliverNavHost
import app.sliver.core.theme.SliverTheme
import app.sliver.debug.ComponentGallery

/** Set true to show the debug ComponentGallery instead of the app. */
const val SHOW_GALLERY = false

@Composable
fun App() {
    SliverTheme {
        if (SHOW_GALLERY) ComponentGallery()
        else SliverNavHost()
    }
}
