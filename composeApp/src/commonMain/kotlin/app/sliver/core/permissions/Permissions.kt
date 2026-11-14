package app.sliver.core.permissions

import androidx.compose.runtime.Composable
import app.sliver.core.APP_NAME

enum class PermissionKind { CameraMic, Notifications, CoarseLocation, MediaLibrary }

enum class PermissionStatus {
    NotAsked, Granted, Partial, Denied;

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
        "Let $APP_NAME see and hear your day",
        "One clip a day. Camera and microphone stay on your phone.",
        "Allow camera & mic",
    )
    PermissionKind.Notifications -> PermissionCopy(
        "A gentle nudge each day",
        "$APP_NAME reminds you once a day to capture your clip.",
        "Allow notifications",
    )
    PermissionKind.MediaLibrary -> PermissionCopy(
        "Let $APP_NAME read your gallery",
        "Used only when you start an import, to find a photo or video for each empty day. Nothing leaves your phone.",
        "Allow photos & videos",
    )
    PermissionKind.CoarseLocation -> PermissionCopy(
        "Know when the light is good",
        "Approximate location is used on this phone only, to time golden hour.",
        "Allow approximate location",
    )
}
