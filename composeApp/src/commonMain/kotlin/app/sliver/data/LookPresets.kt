package app.sliver.data

import app.sliver.data.look.LookModel

/** How the date is drawn on top of a film: [LED] orange segmented digits, [CAMCORDER] tape-deck style. */
enum class StampStyle { PLAIN, LED, CAMCORDER }

data class LookInfo(val label: String, val lutFile: String?, val model: LookModel, val stamp: StampStyle = StampStyle.PLAIN)

/**
 * The shipped filters. Each model is the candidate of the same number in `looks/candidates.json` (a unit test keeps them equal);
 * the other candidates stay in the pool until the owner keeps them.
 */
val LookPreset.info: LookInfo
    get() = when (this) {
        LookPreset.NATURAL -> LookInfo("Natural", null, LookModel())
        LookPreset.KIOSK -> LookInfo(
            "Kiosk",
            "luts/kiosk.cube",
            LookModel(
                temp = 0.35, contrast = 0.25, blackLift = 0.07, saturation = 1.05,
                bands = listOf(1.1, 1.1, 0.95, 1.0, 0.9, 1.0),
                shadowTint = listOf(-0.03, 0.02, 0.025), highlightTint = listOf(0.03, 0.01, -0.04),
                grain = 0.45, grainSize = 2.0, grainChroma = 0.7, halation = 0.15, vignette = 0.5, flash = 0.35,
                softness = 0.5, weave = 0.4, leak = 0.2,
            ),
            StampStyle.LED,
        )
        LookPreset.AFTERHOURS -> LookInfo(
            "Afterhours",
            "luts/afterhours.cube",
            LookModel(
                exposure = -0.3, gamma = listOf(1.15, 1.1, 1.05), contrast = 0.45, saturation = 0.7,
                shadowTint = listOf(-0.04, 0.03, 0.04),
                grain = 0.4, grainSize = 2.0, grainChroma = 0.35, halation = 0.6, vignette = 0.75,
            ),
        )
        LookPreset.SILVER -> LookInfo(
            "Silver",
            "luts/silver.cube",
            LookModel(
                mono = listOf(0.5, 0.4, 0.1),
                gamma = listOf(1.05, 1.05, 1.05),
                contrast = 0.55,
                grain = 0.5,
                grainSize = 2.0,
                halation = 0.15,
                vignette = 0.35,
            ),
        )
        LookPreset.SUNDAY -> LookInfo(
            "Sunday",
            "luts/sunday.cube",
            LookModel(
                temp = 0.2, tint = 0.05, contrast = -0.2, blackLift = 0.05, shoulder = 0.5, saturation = 0.88,
                bands = listOf(1.0, 0.95, 0.85, 0.9, 0.9, 1.0), highlightTint = listOf(0.03, 0.015, -0.02),
                grain = 0.2, grainSize = 1.5, grainChroma = 0.4, bloom = 0.1, vignette = 0.2, fade = 0.2,
            ),
        )
        LookPreset.TAPE -> LookInfo(
            "Tape",
            "luts/tape.cube",
            LookModel(
                tint = 0.05, blackLift = 0.05, saturation = 1.15, shadowTint = listOf(0.01, 0.0, 0.03),
                grain = 0.25, grainSize = 2.0, grainChroma = 0.6, vignette = 0.25, softness = 0.25, weave = 0.6,
                scanlines = 0.5, chromaBleed = 0.8, fade = 0.2,
            ),
            StampStyle.CAMCORDER,
        )
        LookPreset.HALO -> LookInfo(
            "Halo",
            "luts/halo.cube",
            LookModel(
                temp = -0.1, contrast = 0.05, shadowTint = listOf(-0.03, 0.0, 0.05),
                grain = 0.25, grainSize = 1.75, grainChroma = 0.5, halation = 0.6, bloom = 0.8, vignette = 0.35,
                softness = 0.5, fade = 0.1,
            ),
        )
        else -> candidateNumber!!.let { n ->
            val c = LookCandidates.first { it.number == n }
            LookInfo(c.name, null, c.model)
        }
    }

/** Picker groups, in order (owner 14 Nov: think like a filter app — clean everyday looks first, then film, lo-fi, B&W). */
enum class FilterGroup(val label: String) { EVERYDAY("Everyday"), FILM("Film"), CINEMA("Cinematic"), VINTAGE("Vintage"), LOFI("Lo-fi"), MONO("B&W") }

/** Candidate numbers per group, best first. */
private val GroupOrder: List<Pair<FilterGroup, List<Int>>> = listOf(
    FilterGroup.EVERYDAY to listOf(41, 53, 51, 44, 46, 45, 8, 54),
    FilterGroup.FILM to listOf(1, 2, 7, 6, 9, 12, 4, 5, 3, 14),
    FilterGroup.CINEMA to listOf(42, 50, 43, 10, 11, 33, 13, 34, 35, 39),
    FilterGroup.VINTAGE to listOf(49, 48, 36, 18, 16, 17, 15, 37),
    FilterGroup.LOFI to listOf(47, 29, 30, 31, 32, 22, 20, 21, 19, 40),
    FilterGroup.MONO to listOf(52, 24, 23, 26, 25, 38, 27, 28),
)

/** Every filter in the picker, grouped by [FilterGroup]. */
val FilterPresets: List<LookPreset> = GroupOrder.flatMap { (_, numbers) -> numbers.map(::candidatePreset) }

val LookPreset.group: FilterGroup? get() = candidateNumber?.let { n -> GroupOrder.firstOrNull {
    n in it.second
}?.first }

/** Name under a picker tile (no numbers: owner 14 Nov). */
val LookPreset.pickerLabel: String get() = info.label

/** Preset number in `looks/candidates.json`. */
val LookPreset.candidateNumber: Int?
    get() = when (this) {
        LookPreset.NATURAL -> null
        LookPreset.KIOSK -> 1
        LookPreset.AFTERHOURS -> 33
        LookPreset.SILVER -> 23
        LookPreset.SUNDAY -> 6
        LookPreset.TAPE -> 29
        LookPreset.HALO -> 34
        else -> name.removePrefix("C").toInt()
    }

/** [grain] 0..100 overrides the preset's own grain amount. */
fun LookPreset.toParams(intensity: Int = 100, grain: Int? = null): LookParams =
    LookParams(preset = this, intensity = intensity, grain = grain ?: (info.model.grain * 100).toInt())

/** Date style of a film: the user's choice, else the filter's own, else plain. */
fun app.sliver.data.db.Film.effectiveStamp(): StampStyle = stampStyle ?: look?.info?.stamp ?: StampStyle.PLAIN

/** The film-level filter, or null when the film is clean. */
fun app.sliver.data.db.Film.lookParams(): LookParams? = look?.takeIf {
    it != LookPreset.NATURAL
}?.toParams(lookIntensity, lookGrain)
