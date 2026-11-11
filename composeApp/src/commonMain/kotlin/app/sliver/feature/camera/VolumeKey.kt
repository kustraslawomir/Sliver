package app.sliver.feature.camera

import androidx.compose.runtime.Composable

/** While [enabled], a volume key press calls [onPress] instead of changing the volume. */
@Composable
expect fun VolumeKeyEffect(enabled: Boolean, onPress: () -> Unit)
