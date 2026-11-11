package app.sliver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.core.theme.SliverColors

@Composable
fun App() {
    Box(Modifier.fillMaxSize().background(SliverColors.Background))
}
