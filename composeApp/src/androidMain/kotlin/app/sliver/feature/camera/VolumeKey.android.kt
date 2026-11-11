package app.sliver.feature.camera

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

object VolumeKeys {
    var handler: (() -> Unit)? = null

    /** True when the event was consumed. */
    fun dispatch(event: KeyEvent): Boolean {
        val h = handler ?: return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) h()
        return true
    }
}

@Composable
actual fun VolumeKeyEffect(enabled: Boolean, onPress: () -> Unit) {
    val latest = rememberUpdatedState(onPress)
    DisposableEffect(enabled) {
        if (enabled) VolumeKeys.handler = { latest.value() }
        onDispose { VolumeKeys.handler = null }
    }
}
