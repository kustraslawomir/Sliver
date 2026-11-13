package app.sliver.data

import app.sliver.data.look.LookModel

/** GENERATED from looks/candidates.json (owner: all 40 filters in the picker, with numbers). Do not edit by hand. */
data class LookCandidate(val number: Int, val name: String, val model: LookModel)

val LookCandidates: List<LookCandidate> = listOf(
    LookCandidate(1, "Kiosk 400", LookModel(temp = 0.35, contrast = 0.25, blackLift = 0.07, saturation = 1.05, bands = listOf(1.1, 1.1, 0.95, 1.0, 0.9, 1.0), shadowTint = listOf(-0.03, 0.02, 0.025), highlightTint = listOf(0.03, 0.01, -0.04), grain = 0.65, grainSize = 2.0, grainChroma = 0.7, halation = 0.15, vignette = 0.5, flash = 0.35, softness = 0.5, weave = 0.4, leak = 0.2)),
    LookCandidate(2, "Kiosk 200", LookModel(temp = 0.5, tint = 0.05, contrast = 0.15, blackLift = 0.06, saturation = 1.05, bands = listOf(1.1, 1.2, 1.0, 1.0, 0.95, 1.0), highlightTint = listOf(0.04, 0.025, -0.05), grain = 0.45, grainSize = 1.75, grainChroma = 0.6, flash = 0.2, vignette = 0.4, softness = 0.35, leak = 0.1)),
    LookCandidate(3, "Plus 200", LookModel(temp = 0.3, tint = -0.1, gamma = listOf(1.0, 1.0, 1.05), contrast = 0.15, blackLift = 0.07, saturation = 0.8, bands = listOf(1.0, 1.0, 0.8, 0.9, 0.95, 1.0), grain = 0.8, grainSize = 2.1, grainChroma = 0.8, vignette = 0.3, softness = 0.3)),
    LookCandidate(4, "Superstore", LookModel(temp = -0.1, tint = 0.12, contrast = 0.25, blackLift = 0.04, saturation = 1.05, bands = listOf(1.1, 0.9, 1.1, 1.1, 1.0, 1.1), shadowTint = listOf(-0.03, 0.03, 0.03), grain = 0.5, grainSize = 1.75, grainChroma = 0.6, vignette = 0.3)),
    LookCandidate(5, "Corner 100", LookModel(temp = 0.05, contrast = -0.1, blackLift = 0.06, shoulder = 0.4, saturation = 0.9, shadowTint = listOf(-0.03, 0.03, 0.025), highlightTint = listOf(0.03, 0.02, 0.03), grain = 0.35, grainSize = 1.5, grainChroma = 0.5, vignette = 0.25)),
    LookCandidate(6, "Sunday 160", LookModel(temp = 0.2, tint = 0.05, contrast = -0.2, blackLift = 0.05, shoulder = 0.5, saturation = 0.88, bands = listOf(1.0, 0.95, 0.85, 0.9, 0.9, 1.0), highlightTint = listOf(0.03, 0.015, -0.02), grain = 0.2, grainSize = 1.5, grainChroma = 0.4, bloom = 0.1, vignette = 0.2, fade = 0.2)),
    LookCandidate(7, "Sunday 400", LookModel(temp = 0.28, blackLift = 0.04, shoulder = 0.3, saturation = 0.95, highlightTint = listOf(0.035, 0.015, -0.03), grain = 0.3, grainSize = 1.5, grainChroma = 0.5, bloom = 0.05, vignette = 0.25)),
    LookCandidate(8, "Vivid 100", LookModel(temp = 0.05, contrast = 0.3, shoulder = 0.3, saturation = 1.15, bands = listOf(1.1, 1.05, 1.1, 1.1, 1.25, 1.0), grain = 0.12, grainSize = 1.25, grainChroma = 0.3, vignette = 0.2)),
    LookCandidate(9, "Studio 400", LookModel(temp = -0.1, tint = 0.06, contrast = 0.15, blackLift = 0.03, bands = listOf(1.0, 0.95, 1.15, 1.05, 1.0, 1.15), grain = 0.3, grainSize = 1.5, grainChroma = 0.5, vignette = 0.2)),
    LookCandidate(10, "Afterhours", LookModel(exposure = -0.3, temp = -0.35, contrast = 0.3, saturation = 0.95, shadowTint = listOf(-0.03, 0.0, 0.05), grain = 0.5, grainSize = 2.0, grainChroma = 0.4, halation = 0.7, vignette = 0.45)),
    LookCandidate(11, "Afterhours Warm", LookModel(exposure = -0.3, temp = 0.05, contrast = 0.3, saturation = 0.95, shadowTint = listOf(-0.02, 0.0, 0.04), highlightTint = listOf(0.04, 0.0, -0.03), grain = 0.5, grainSize = 2.0, grainChroma = 0.4, halation = 0.7, vignette = 0.45)),
    LookCandidate(12, "Reel 250", LookModel(temp = 0.05, contrast = -0.1, blackLift = 0.03, shoulder = 0.8, saturation = 0.95, grain = 0.25, grainSize = 1.5, grainChroma = 0.3, halation = 0.2, vignette = 0.2)),
    LookCandidate(13, "Bleach", LookModel(contrast = 0.6, shoulder = 0.2, saturation = 0.4, highlightTint = listOf(0.01, 0.015, 0.03), grain = 0.45, grainSize = 1.75, grainChroma = 0.1, vignette = 0.35)),
    LookCandidate(14, "Slide 100", LookModel(contrast = 0.4, shoulder = 0.3, gamma = listOf(1.05, 1.05, 1.05), saturation = 1.25, bands = listOf(1.1, 1.0, 1.1, 1.1, 1.3, 1.1), grain = 0.1, grainSize = 1.25, vignette = 0.25)),
    LookCandidate(15, "Cross", LookModel(gamma = listOf(0.9, 1.0, 1.15), contrast = 0.55, saturation = 1.15, shadowTint = listOf(-0.04, 0.02, 0.05), highlightTint = listOf(0.05, 0.06, -0.06), grain = 0.4, grainSize = 1.75, grainChroma = 0.6, vignette = 0.35)),
    LookCandidate(16, "Slide Fade", LookModel(temp = 0.1, tint = 0.2, contrast = 0.1, blackLift = 0.1, saturation = 0.85, shadowTint = listOf(0.03, -0.02, 0.02), grain = 0.35, grainSize = 1.75, grainChroma = 0.6, vignette = 0.3, fade = 0.2)),
    LookCandidate(17, "Expired 03", LookModel(temp = 0.2, tint = 0.25, gamma = listOf(1.0, 1.05, 1.0), contrast = 0.1, blackLift = 0.09, saturation = 0.95, shadowTint = listOf(0.04, 0.0, 0.03), highlightTint = listOf(0.05, -0.01, -0.03), grain = 0.9, grainSize = 2.2, grainChroma = 0.85, vignette = 0.5, fade = 0.25, leak = 0.1)),
    LookCandidate(18, "Attic", LookModel(temp = 0.5, contrast = -0.25, blackLift = 0.08, shoulder = 0.5, saturation = 0.6, bands = listOf(1.0, 1.2, 1.0, 1.0, 1.0, 1.0), highlightTint = listOf(0.05, 0.04, -0.05), grain = 0.5, grainSize = 2.0, grainChroma = 0.3, bloom = 0.1, vignette = 0.55, softness = 0.6, fade = 0.3)),
    LookCandidate(19, "Leak", LookModel(temp = 0.35, contrast = 0.15, blackLift = 0.04, saturation = 1.15, grain = 0.4, grainSize = 1.75, grainChroma = 0.5, vignette = 0.35, softness = 0.2, weave = 0.3, leak = 1.0)),
    LookCandidate(20, "Kiosk Flash", LookModel(temp = 0.4, contrast = 0.3, blackLift = 0.06, saturation = 1.05, bands = listOf(1.15, 1.1, 0.95, 1.0, 0.9, 1.0), highlightTint = listOf(0.04, 0.01, -0.04), shadowTint = listOf(-0.03, 0.02, 0.025), grain = 0.6, grainSize = 2.0, grainChroma = 0.7, flash = 1.0, vignette = 0.5, softness = 0.4, weave = 0.3)),
    LookCandidate(21, "Kiosk Night", LookModel(exposure = -0.6, temp = -0.05, tint = -0.2, gamma = listOf(1.1, 1.0, 1.1), contrast = 0.3, blackLift = 0.05, grain = 1.0, grainSize = 2.4, grainChroma = 0.9, halation = 0.3, vignette = 0.6, softness = 0.5)),
    LookCandidate(22, "Toy", LookModel(temp = 0.1, contrast = 0.3, saturation = 1.2, grain = 0.45, grainSize = 1.75, grainChroma = 0.5, bloom = 0.1, vignette = 0.85, softness = 1.0)),
    LookCandidate(23, "Silver 400", LookModel(mono = listOf(0.5, 0.4, 0.1), gamma = listOf(1.05, 1.05, 1.05), contrast = 0.55, grain = 0.7, grainSize = 2.0, halation = 0.15, vignette = 0.35)),
    LookCandidate(24, "Silver 100", LookModel(mono = listOf(0.45, 0.45, 0.1), contrast = 0.25, shoulder = 0.3, grain = 0.25, grainSize = 1.4, vignette = 0.2)),
    LookCandidate(25, "Silver 3200", LookModel(exposure = -0.2, mono = listOf(0.5, 0.4, 0.1), gamma = listOf(1.15, 1.15, 1.15), contrast = 0.7, grain = 1.0, grainSize = 2.8, halation = 0.3, vignette = 0.5)),
    LookCandidate(26, "Silver Soft", LookModel(mono = listOf(0.5, 0.4, 0.1), contrast = -0.1, blackLift = 0.09, shoulder = 0.4, grain = 0.4, grainSize = 1.75, bloom = 0.1, vignette = 0.25, fade = 0.15)),
    LookCandidate(27, "Sepia", LookModel(mono = listOf(0.5, 0.4, 0.1), contrast = 0.2, blackLift = 0.04, shadowTint = listOf(0.06, 0.03, -0.03), highlightTint = listOf(0.08, 0.05, -0.06), grain = 0.4, grainSize = 1.75, vignette = 0.35)),
    LookCandidate(28, "Cyan", LookModel(mono = listOf(0.4, 0.45, 0.15), contrast = 0.3, blackLift = 0.03, shadowTint = listOf(-0.06, 0.0, 0.07), highlightTint = listOf(-0.05, 0.02, 0.06), grain = 0.3, grainSize = 1.75, vignette = 0.3)),
    LookCandidate(29, "Tape", LookModel(tint = 0.05, blackLift = 0.05, saturation = 1.15, shadowTint = listOf(0.01, 0.0, 0.03), grain = 0.35, grainSize = 2.0, grainChroma = 0.6, vignette = 0.25, softness = 0.25, weave = 0.6, scanlines = 0.5, chromaBleed = 0.8, fade = 0.2)),
    LookCandidate(30, "Hi8", LookModel(temp = -0.15, contrast = 0.2, grain = 0.2, grainChroma = 0.3, vignette = 0.2, softness = 0.1, weave = 0.5, scanlines = 0.3, chromaBleed = 0.4)),
    LookCandidate(31, "DV 2004", LookModel(temp = -0.2, gamma = listOf(0.92, 0.92, 0.92), contrast = 0.45, saturation = 1.1, grain = 0.15, grainSize = 1.5, vignette = 0.1, scanlines = 0.15, chromaBleed = 0.1)),
    LookCandidate(32, "Webcam", LookModel(temp = -0.05, tint = -0.3, gamma = listOf(1.0, 0.92, 1.1), contrast = 0.1, blackLift = 0.06, saturation = 0.75, highlightTint = listOf(0.03, 0.04, -0.04), grain = 1.0, grainSize = 2.2, grainChroma = 0.8, softness = 0.6, weave = 0.5, scanlines = 0.1)),
    LookCandidate(33, "Afterhours Dark", LookModel(exposure = -0.3, gamma = listOf(1.15, 1.1, 1.05), contrast = 0.45, saturation = 0.7, shadowTint = listOf(-0.04, 0.03, 0.04), grain = 0.65, grainSize = 2.0, grainChroma = 0.35, halation = 0.6, vignette = 0.75)),
    LookCandidate(34, "Halo", LookModel(temp = -0.1, contrast = 0.05, shadowTint = listOf(-0.03, 0.0, 0.05), grain = 0.4, grainSize = 1.75, grainChroma = 0.5, halation = 0.6, bloom = 0.8, vignette = 0.35, softness = 0.5, fade = 0.1)),
    LookCandidate(35, "Fog", LookModel(temp = -0.15, contrast = -0.4, blackLift = 0.1, shoulder = 0.3, saturation = 0.75, grain = 0.2, grainSize = 1.5, bloom = 0.35, vignette = 0.15, fade = 0.35)),
    LookCandidate(36, "Golden", LookModel(temp = 0.6, contrast = 0.2, shoulder = 0.3, saturation = 1.1, bands = listOf(1.1, 1.2, 1.0, 1.0, 1.0, 1.0), highlightTint = listOf(0.07, 0.03, -0.07), grain = 0.25, grainSize = 1.5, halation = 0.2, bloom = 0.15, vignette = 0.3)),
    LookCandidate(37, "Winter", LookModel(exposure = 0.2, temp = -0.5, contrast = 0.1, saturation = 0.6, shadowTint = listOf(-0.02, 0.0, 0.04), highlightTint = listOf(-0.02, 0.0, 0.03), grain = 0.2, grainSize = 1.5, vignette = 0.15)),
    LookCandidate(38, "Noir Tint", LookModel(mono = listOf(0.45, 0.4, 0.15), gamma = listOf(1.1, 1.1, 1.1), contrast = 0.6, shadowTint = listOf(-0.05, 0.04, 0.05), grain = 0.55, grainSize = 2.0, halation = 0.2, vignette = 0.5)),
    LookCandidate(39, "Neon", LookModel(exposure = -0.2, contrast = 0.3, saturation = 1.25, bands = listOf(1.0, 0.9, 0.9, 1.35, 1.2, 1.5), shadowTint = listOf(0.02, -0.02, 0.05), grain = 0.3, grainSize = 1.75, halation = 0.3, bloom = 0.6, vignette = 0.4)),
    LookCandidate(40, "Plain", LookModel(grain = 0.5, grainSize = 1.75)),
)

/** Enum entry for each candidate number; the six first-shipped ones keep their names. */
fun candidatePreset(number: Int): LookPreset = when (number) {
    1 -> LookPreset.KIOSK
    33 -> LookPreset.AFTERHOURS
    23 -> LookPreset.SILVER
    6 -> LookPreset.SUNDAY
    29 -> LookPreset.TAPE
    34 -> LookPreset.HALO
    else -> LookPreset.valueOf("C" + number.toString().padStart(2, '0'))
}
