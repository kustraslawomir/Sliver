package app.sliver.core.permissions

import androidx.compose.runtime.Composable

enum class PermissionKind { CameraMic, Notifications, CoarseLocation }

enum class PermissionStatus {
    NotAsked, Granted, Denied;

    val isUsable get() = this == Granted
}

interface PermissionController {
    fun status(kind: PermissionKind): PermissionStatus
    fun request(kind: PermissionKind)
    fun openSettings()
}

/** Statuses are re-read after every request result and when returning from settings. */
@Composable
expect fun rememberPermissionController(): PermissionController

data class PermissionCopy(val title: String, val body: String, val allow: String)

fun PermissionKind.copy() = when (this) {
    PermissionKind.CameraMic -> PermissionCopy(
        "Let Fragments see and hear your day",
        "One clip a day. Camera and microphone stay on your phone.",
        "Allow camera & mic",
    )
    PermissionKind.Notifications -> PermissionCopy(
        "A gentle nudge each day",
        "Fragments reminds you once a day to capture your clip.",
        "Allow notifications",
    )
    PermissionKind.CoarseLocation -> PermissionCopy(
        "Know when the light is good",
        "Approximate location is used on this phone only, to time golden hour.",
        "Allow approximate location",
    )
}
