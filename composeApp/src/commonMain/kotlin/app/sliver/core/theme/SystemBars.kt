package app.sliver.core.theme

import androidx.compose.runtime.Composable

/** Status and navigation bar icons: dark icons when the page is light. */
@Composable
expect fun SystemBarsEffect(lightBackground: Boolean)
