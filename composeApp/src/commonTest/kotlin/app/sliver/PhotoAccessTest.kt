package app.sliver

import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.photoStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoAccessTest {
    private val sel = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    private val img = "android.permission.READ_MEDIA_IMAGES"

    @Test fun partialOnly() = assertEquals(PermissionStatus.Partial, photoStatus(34, setOf(sel), true))
    @Test fun fullWins() = assertEquals(PermissionStatus.Granted, photoStatus(34, setOf(sel, img), true))
    @Test fun deniedAfterAsk() = assertEquals(PermissionStatus.Denied, photoStatus(34, emptySet(), true))
    @Test fun notAsked() = assertEquals(PermissionStatus.NotAsked, photoStatus(33, emptySet(), false))
    @Test fun legacy() = assertEquals(
        PermissionStatus.Granted, photoStatus(30, setOf("android.permission.READ_EXTERNAL_STORAGE"), false),
    )
}
