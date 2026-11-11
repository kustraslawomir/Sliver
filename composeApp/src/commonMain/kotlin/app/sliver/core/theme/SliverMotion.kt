package app.sliver.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.sqrt

object SliverMotion {
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseEmphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)
    val EaseExit = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    const val Instant = 100
    const val Quick = 160
    const val Base = 240
    const val Slow = 400
    const val Drop = 650
    const val Cinematic = 1600

    fun <T> standard(ms: Int = Base): FiniteAnimationSpec<T> = tween(ms, easing = EaseStandard)
    fun <T> emphasized(ms: Int = Slow): FiniteAnimationSpec<T> = tween(ms, easing = EaseEmphasized)
    fun <T> exit(ms: Int = Quick): FiniteAnimationSpec<T> = tween(ms, easing = EaseExit)

    // Damping ratio = damping / (2 * sqrt(stiffness * mass)), mass = 1.
    fun <T> springLockIn(): FiniteAnimationSpec<T> = spring(dampingRatio = 28f / (2f * sqrt(420f)), stiffness = 420f)
    fun <T> springDrop(): FiniteAnimationSpec<T> = spring(dampingRatio = 22f / (2f * sqrt(260f)), stiffness = 260f)
}
