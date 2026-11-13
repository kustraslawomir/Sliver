package app.sliver.core.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

enum class GlassStyle { Regular, Strong }

/** The app's backdrop (App.kt marks the navigation host as the haze source). Null in previews and tests. */
val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

/** Real blur needs API 31+ and no battery saver; otherwise glass is the solid fallback. */
@Composable
expect fun rememberGlassBlur(): Boolean

/** The lit edge that makes it read as glass: 1 dp, white 30 % at the top → 6 % at the bottom. */
internal fun glassEdge(light: Boolean) =
    if (light) Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.7f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f)))

/**
 * v1.7 glass (GlassKit §2). Over Compose content: Haze backdrop blur (regular 24 dp + white 8 %, strong 32 dp + white
 * 12 %, noise 0.08). [overMedia]: video and the camera are SurfaceViews the blur cannot read, so the glass there is the
 * tint over black 22 % (the "bright content" layer from CameraV17). Fallback: #1C1C1F at 92 % (light: white 92 %).
 * [backdrop] must be a haze source that is a sibling of this glass, not an ancestor: Haze draws nothing for an effect
 * inside its own source. Without it (sheets, which live inside the screen) the glass is the solid fallback.
 * DECISION: Haze 1.6 has no saturation control; saturation 1.6/1.8 is left out.
 */
fun Modifier.glass(style: GlassStyle, shape: Shape, overMedia: Boolean = false, backdrop: HazeState? = null): Modifier = composed {
    val light = SliverColors.light
    val state = backdrop
    val blur = rememberGlassBlur()
    val strong = style == GlassStyle.Strong
    val shadow = if (light) shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f)) // lint-ok: elevation
    else shadow(if (strong) 16.dp else 10.dp, shape, ambientColor = Color.Black.copy(alpha = if (strong) 0.45f else 0.35f), spotColor = Color.Black.copy(alpha = if (strong) 0.45f else 0.35f)) // lint-ok: elevation
    val tint = when {
        light -> Color.White.copy(alpha = 0.62f)
        else -> Color.White.copy(alpha = if (strong) 0.12f else 0.08f)
    }
    val surface = when {
        overMedia && !light -> Modifier.background(Color.Black.copy(alpha = 0.22f), shape).background(tint, shape)
        state != null && blur -> Modifier.hazeEffect(
            state,
            HazeStyle(
                backgroundColor = SliverColors.BgBase,
                tints = listOf(HazeTint(tint)),
                blurRadius = if (strong) 32.dp else 24.dp,
                noiseFactor = 0.08f,
            ),
        )
        light -> Modifier.background(Color.White.copy(alpha = 0.92f), shape)
        else -> Modifier.background(Color(0xFF1C1C1F).copy(alpha = if (strong) 0.97f else 0.92f), shape)
    }
    // A shadow under see-through glass shows through as a smudge, so media glass has none.
    (if (overMedia) this else then(shadow)).clip(shape).then(surface).border(1.dp, glassEdge(light), shape)
}
