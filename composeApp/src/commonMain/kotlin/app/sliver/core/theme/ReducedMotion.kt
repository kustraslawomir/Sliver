package app.sliver.core.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable

/** True when the system asks for no animation (Android: animator duration scale 0 / Remove animations). */
@Composable
expect fun rememberReducedMotion(): Boolean

/** [spec] normally, an instant jump to the end state under reduced motion. */
fun <T> motionSpec(reduced: Boolean, spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (reduced) snap() else spec
