package app.sliver.data.look

import kotlinx.serialization.Serializable

/**
 * One parametric look: a colour part baked into a 33^3 LUT ([LutGenerator]) and a shader part run by the GL pass.
 * Every number is our own value built from general photographic techniques (white balance, tone curves, black lift,
 * shoulder roll-off, per-hue saturation, split tint, channel mix); defaults are the identity.
 */
@Serializable
data class LookModel(
    /** Exposure in EV applied before everything else. */
    val exposure: Double = 0.0,
    /** White balance: -1 cool .. +1 warm. */
    val temp: Double = 0.0,
    /** -1 green .. +1 magenta. */
    val tint: Double = 0.0,
    /** Per-channel tone curve exponents (r, g, b); above 1 darkens that channel's mids. */
    val gamma: List<Double> = emptyList(),
    /** S-curve strength, -1 flat .. +1 hard. */
    val contrast: Double = 0.0,
    /** Black lift, 0..0.2. */
    val blackLift: Double = 0.0,
    /** Highlight roll-off, 0..1. */
    val shoulder: Double = 0.0,
    val saturation: Double = 1.0,
    /** Saturation multiplier per hue band: red, yellow, green, cyan, blue, magenta. */
    val bands: List<Double> = emptyList(),
    /** Signed RGB shift added in the shadows / highlights. */
    val shadowTint: List<Double> = emptyList(),
    val highlightTint: List<Double> = emptyList(),
    /** Black and white channel mix (r, g, b); empty keeps colour. */
    val mono: List<Double> = emptyList(),

    val grain: Double = 0.0,
    /** Grain cell in px at 1080p; scales with output height. */
    val grainSize: Double = 1.75,
    /** 0 mono grain .. 1 full colour grain. */
    val grainChroma: Double = 0.0,
    val halation: Double = 0.0,
    val bloom: Double = 0.0,
    val vignette: Double = 0.0,
    val flash: Double = 0.0,
    /** Soft plastic lens: edge blur, slight barrel distortion, chromatic aberration at the edges. */
    val softness: Double = 0.0,
    /** Gate weave and exposure flicker amount, 0..1 (1 = +-0.5 px and +-1.5 %). */
    val weave: Double = 0.0,
    /** Probability that a clip gets a light leak. */
    val leak: Double = 0.0,
    val scanlines: Double = 0.0,
    val chromaBleed: Double = 0.0,
    val fade: Double = 0.0,
) {
    /** The colour part only (what the LUT is built from). */
    fun colourOnly(): LookModel = LookModel(
        exposure, temp, tint, gamma, contrast, blackLift, shoulder, saturation, bands, shadowTint, highlightTint, mono,
    )

    val hasLut: Boolean get() = colourOnly() != LookModel()
}

/** A named, numbered entry of the candidate pool (`looks/candidates.json`). [note] is an internal description, never shown. */
@Serializable
data class LookCandidate(
    val number: Int,
    val name: String,
    val family: String,
    val note: String = "",
    val model: LookModel = LookModel(),
)
