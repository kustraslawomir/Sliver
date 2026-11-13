package app.sliver.core.ui

import app.sliver.core.icons.ReelogWordmark
import app.sliver.core.icons.WORDMARK_DISC_R
import app.sliver.core.icons.WORDMARK_DISC_X
import kotlin.math.pow

/** Timeline of the cold-start splash, design/motion/SplashReelog.html → frame(t). The system splash covers 0–260 ms. */
const val SPLASH_START_MS = 260
const val SPLASH_END_MS = 1500
const val SPLASH_REDUCED_HOLD_MS = 600
const val SPLASH_FADE_MS = 300

/** Logo size: the HTML lays the word out at 56 px, the disc is half of that. */
const val SPLASH_FONT_DP = 56f
const val SPLASH_DISC_DP = 72f

/** Record today disc on the project screen (72 dp − 30 %, owner 2026-11-13); the splash ends at this size. */
const val DOCK_DISC_DP = 50f
const val SPLASH_TAGLINE_GAP_DP = 52f

/** dp per wordmark viewport unit at [SPLASH_FONT_DP]. */
const val SPLASH_WORD_SCALE = SPLASH_FONT_DP * 0.5f / (2 * WORDMARK_DISC_R)

/**
 * One frame, in dp relative to the screen: the disc centre (x from the screen centre, y from the top) and size, the
 * letter offsets in wordmark units, and the opacities.
 */
data class SplashFrame(
    val discDx: Float,
    val discY: Float,
    val discSize: Float,
    val reelShift: Float,
    val gShift: Float,
    val letters: Float,
    val tagline: Float,
    val background: Float,
)

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun ease(x: Float) = 1 - (1 - clamp01(x)).pow(3)
private fun easeInOut(x: Float): Float {
    val k = clamp01(x)
    return if (k < 0.5f) 4 * k * k * k else 1 - (-2 * k + 2).pow(3) / 2
}
private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

/** Disc centre in the finished word, relative to the screen centre (the word is centred). */
val SPLASH_WORD_DISC_DX = (WORDMARK_DISC_X - ReelogWordmark.WIDTH / 2) * SPLASH_WORD_SCALE

/**
 * [centerY] = where the system splash drew the disc (screen centre); [dockY] = centre of the Record today button.
 * Times in ms from process start of the animation (0 = system splash appears).
 */
fun splashFrame(t: Float, centerY: Float, dockY: Float): SplashFrame {
    val m = easeInOut((t - 260) / 380)
    val tag = ease((t - 640) / 300)
    val out = easeInOut((t - 1100) / 400)
    val fadeLetters = 1 - ease((t - 1100) / 200)
    val wordDisc = 2 * WORDMARK_DISC_R * SPLASH_WORD_SCALE
    val inWord = t <= 1100
    return SplashFrame(
        discDx = if (inWord) lerp(0f, SPLASH_WORD_DISC_DX, m) else lerp(SPLASH_WORD_DISC_DX, 0f, out),
        discY = if (inWord) centerY else lerp(centerY, dockY, out),
        discSize = if (inWord) lerp(SPLASH_DISC_DP, wordDisc, m) else lerp(wordDisc, DOCK_DISC_DP, out),
        reelShift = lerp(WORDMARK_DISC_X - ReelogWordmark.REEL_RIGHT, 0f, m),
        gShift = lerp(WORDMARK_DISC_X - ReelogWordmark.G_LEFT, 0f, m),
        letters = m * fadeLetters,
        tagline = tag * fadeLetters,
        background = 1 - ease((t - 1200) / 300),
    )
}
