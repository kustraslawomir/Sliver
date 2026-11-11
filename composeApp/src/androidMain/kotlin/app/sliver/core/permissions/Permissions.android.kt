package app.sliver.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private fun permissionsFor(kind: PermissionKind, sdk: Int): List<String> = when (kind) {
    PermissionKind.CameraMic -> listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    PermissionKind.Notifications -> if (sdk >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
    PermissionKind.Photos -> when {
        sdk >= 34 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        sdk >= 33 -> listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private class AndroidPermissionController(
    private val context: Context,
    private val launch: (Array<String>) -> Unit,
) : PermissionController {
    private val prefs = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)
    private val sdk = Build.VERSION.SDK_INT

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    override fun status(kind: PermissionKind): PermissionStatus {
        val perms = permissionsFor(kind, sdk)
        val asked = prefs.getBoolean(kind.name, false)
        return when {
            kind == PermissionKind.Photos -> photoStatus(sdk, perms.filter(::granted).toSet(), asked)
            perms.all(::granted) -> PermissionStatus.Granted
            asked -> PermissionStatus.Denied
            else -> PermissionStatus.NotAsked
        }
    }

    override fun request(kind: PermissionKind) {
        prefs.edit().putBoolean(kind.name, true).apply()
        launch(permissionsFor(kind, sdk).toTypedArray())
    }

    override fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { version++ }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) version++ }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    // New instance per version so callers recompose and re-read statuses.
    return remember(version) {
        AndroidPermissionController(context) { perms ->
            if (perms.isEmpty()) version++ else launcher.launch(perms)
        }
    }
}
