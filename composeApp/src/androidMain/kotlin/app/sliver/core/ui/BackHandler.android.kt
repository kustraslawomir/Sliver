package app.sliver.core.ui

import androidx.activity.compose.BackHandler as ActivityBackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = ActivityBackHandler(enabled, onBack)
