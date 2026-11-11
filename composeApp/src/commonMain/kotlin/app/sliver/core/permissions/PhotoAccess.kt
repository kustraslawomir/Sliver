package app.sliver.core.permissions

/** Pure mapping of granted Android photo permissions to a status. */
fun photoStatus(sdk: Int, granted: Set<String>, asked: Boolean): PermissionStatus = when {
    sdk >= 33 && ("android.permission.READ_MEDIA_IMAGES" in granted || "android.permission.READ_MEDIA_VIDEO" in granted) ->
        PermissionStatus.Granted
    sdk >= 34 && "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" in granted -> PermissionStatus.Partial
    sdk < 33 && "android.permission.READ_EXTERNAL_STORAGE" in granted -> PermissionStatus.Granted
    asked -> PermissionStatus.Denied
    else -> PermissionStatus.NotAsked
}
