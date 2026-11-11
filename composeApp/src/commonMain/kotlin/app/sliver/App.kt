package app.sliver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverTheme
import app.sliver.debug.ComponentGallery

/** Set true to show the debug ComponentGallery instead of the app. */
const val SHOW_GALLERY = false

@Composable
fun App() {
    SliverTheme {
        if (SHOW_GALLERY) ComponentGallery()
        else Box(Modifier.fillMaxSize().background(SliverColors.BgBase))
    }
}
