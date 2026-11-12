package app.sliver.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner

/** False while the screen is locked or the app is in the background; players must not run then. */
@Composable
fun rememberAppVisible(): Boolean {
    val owner = LocalLifecycleOwner.current
    var visible by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    LifecycleStartEffect(Unit) {
        visible = true
        onStopOrDispose { visible = false }
    }
    return visible
}

/** Runs [action] when the screen locks or the app goes to the background, e.g. to flip a play button to paused. */
@Composable
fun OnAppHidden(action: () -> Unit) = LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = action)
