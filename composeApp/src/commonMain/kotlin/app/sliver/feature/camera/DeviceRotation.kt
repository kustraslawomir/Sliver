package app.sliver.feature.camera

import androidx.compose.runtime.Composable

/** Device rotation in degrees (0..359, OrientationEventListener convention); 0 while unknown. */
@Composable
expect fun rememberDeviceRotation(): Int
