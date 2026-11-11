package app.sliver.core.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowClass { Compact, Medium, Expanded }

fun windowClassFor(widthDp: Float): WindowClass = when {
    widthDp < 600f -> WindowClass.Compact
    widthDp < 840f -> WindowClass.Medium
    else -> WindowClass.Expanded
}

data class WindowSize(val width: Dp, val height: Dp) {
    val windowClass get() = windowClassFor(width.value)
    val isCompact get() = windowClass == WindowClass.Compact
    val landscape get() = width > height

    /** Roomy and wider than tall: side-by-side layouts. */
    val sideBySide get() = !isCompact && landscape
}

val LocalWindowSize = compositionLocalOf { WindowSize(400.dp, 800.dp) }

val MaxContentWidth = 600.dp
