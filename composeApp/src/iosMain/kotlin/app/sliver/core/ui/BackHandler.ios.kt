package app.sliver.core.ui

import androidx.compose.runtime.Composable

// DECISION: iOS has no system back button; screens are left through their on-screen back controls.
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
