package app.sliver.data.look

import app.sliver.data.LookParams
import app.sliver.data.info
import kotlin.math.max

/**
 * Every uniform the one-pass look shader needs for one frame. Built by [lookFrame] as a pure function of
 * (look, clip seed, frame time, output height), so preview, export and the CPU contact-sheet renderer agree.
 */
data class LookFrame(
    val grain: Float,
    val grainPx: Float,
    val grainChroma: Float,
    val grainSeed: Float,
    val halation: Float,
    val bloom: Float,
    val vignette: Float,
    val flash: Float,
    val soft: Float,
    val scan: Float,
    val scanPeriod: Float,
    val bleed: Float,
    val fade: Float,
    val weaveDx: Float,
    val weaveDy: Float,
    val exposure: Float,
    val leak: LeakState,
) {
    /** True when the shader would change nothing, so the pass can be skipped. */
    val isIdentity: Boolean
        get() = grain == 0f && halation == 0f && bloom == 0f && vignette == 0f && flash == 0f && soft == 0f &&
            scan == 0f && bleed == 0f && fade == 0f && weaveDx == 0f && weaveDy == 0f && exposure == 1f && leak.strength == 0f
}

/**
 * [intensity] 0..1 scales the whole look (LUT blend and every shader amount except grain); [grain] 0..1 is the absolute
 * grain amount. [animate] false (paused preview) freezes the per-frame randomness at frame 0.
 */
fun lookFrame(
    model: LookModel,
    intensity: Float,
    grain: Float,
    seed: Long,
    timeUs: Long,
    outputHeight: Int,
    animate: Boolean = true,
): LookFrame {
    val k = intensity.coerceIn(0f, 1f)
    val frame = if (animate) LookRandom.frameIndex(timeUs) else 0L
    val j = LookRandom.jitter(seed, frame, model.weave.toFloat() * k)
    return LookFrame(
        grain = grain.coerceIn(0f, 1f),
        grainPx = max(1f, (model.grainSize * outputHeight / 1080.0).toFloat()),
        grainChroma = model.grainChroma.toFloat(),
        grainSeed = LookRandom.grainSeed(seed, frame),
        halation = model.halation.toFloat() * k,
        bloom = model.bloom.toFloat() * k,
        vignette = model.vignette.toFloat() * k,
        flash = model.flash.toFloat() * k,
        soft = model.softness.toFloat() * k,
        scan = model.scanlines.toFloat() * k,
        scanPeriod = max(2f, (outputHeight / 360).toFloat()),
        bleed = model.chromaBleed.toFloat() * k,
        fade = model.fade.toFloat() * k,
        weaveDx = j.dx,
        weaveDy = j.dy,
        exposure = if (model.weave > 0.0) 1f + (j.exposure - 1f) else 1f,
        leak = LookRandom.leak(seed, model.leak * k, if (animate) timeUs / 1_000_000f else 0f),
    )
}

fun lookFrame(params: LookParams, seed: Long, timeUs: Long, outputHeight: Int, animate: Boolean = true): LookFrame =
    lookFrame(params.preset.info.model, params.intensity / 100f, params.grain / 100f, seed, timeUs, outputHeight, animate)
