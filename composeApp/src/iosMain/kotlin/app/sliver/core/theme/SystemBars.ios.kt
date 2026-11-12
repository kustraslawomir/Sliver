package app.sliver.core.theme

import androidx.compose.runtime.Composable

// DECISION: status bar style comes from Info.plist (light content on the dark app); no per-screen switching yet.
@Composable
actual fun SystemBarsEffect(lightBackground: Boolean) = Unit
