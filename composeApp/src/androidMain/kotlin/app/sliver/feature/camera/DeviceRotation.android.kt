package app.sliver.feature.camera

import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDeviceRotation(): Int {
    val context = LocalContext.current
    var rotation by remember { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    // Battery (owner 14 Nov): the screen stays composed while the app is in the background or the phone is locked, so
    // the accelerometer ran for minutes in the background. It now runs only while the app is resumed.
    DisposableEffect(context, lifecycle) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation != ORIENTATION_UNKNOWN) rotation = orientation
            }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> if (listener.canDetectOrientation()) listener.enable()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> listener.disable()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            listener.disable()
        }
    }
    return rotation
}
