package app.sliver.core.permissions

import androidx.compose.runtime.Composable

enum class PermissionKind { CameraMic, Photos, Notifications }

enum class PermissionStatus {
    NotAsked, Granted, Partial, Denied;

    val isUsable get() = this == Granted || this == Partial
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
        "Let SLIVER see and hear your day",
        "One clip a day. Camera and microphone stay on your phone.",
        "Allow camera & mic",
    )
    PermissionKind.Photos -> PermissionCopy(
        "Add moments from your gallery",
        "Pick the clips you want in your film. Limited access is fine.",
        "Allow photos",
    )
    PermissionKind.Notifications -> PermissionCopy(
        "A gentle nudge each day",
        "SLIVER reminds you once a day to capture your clip.",
        "Allow notifications",
    )
}
