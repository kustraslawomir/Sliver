package app.sliver.data

data class LookDefaults(val grain: Int = 0, val vignette: Int = 0, val fade: Int = 0, val sharp: Int = 0)

data class LookInfo(val label: String, val lutFile: String?, val defaults: LookDefaults)

val LookPreset.info: LookInfo
    get() = when (this) {
        LookPreset.NATURAL -> LookInfo("Natural", null, LookDefaults())
        LookPreset.NOIR -> LookInfo("Noir", "luts/noir.cube", LookDefaults(grain = 35, vignette = 50, sharp = 30))
        LookPreset.BLACKOUT -> LookInfo("Blackout", "luts/blackout.cube", LookDefaults(vignette = 65, sharp = 40))
        LookPreset.NIGHT_NEON -> LookInfo("Night Neon", "luts/night_neon.cube", LookDefaults(grain = 20, vignette = 40))
        LookPreset.COLD_STEEL -> LookInfo("Cold Steel", "luts/cold_steel.cube", LookDefaults(vignette = 25, sharp = 60))
        LookPreset.EMBER -> LookInfo("Ember", "luts/ember.cube", LookDefaults(grain = 22, vignette = 30))
        LookPreset.VHS_94 -> LookInfo("VHS 94", "luts/vhs_94.cube", LookDefaults(grain = 55, fade = 35))
        LookPreset.HARD_LIGHT -> LookInfo("Hard Light", "luts/hard_light.cube", LookDefaults(vignette = 20, sharp = 90))
        LookPreset.GHOST -> LookInfo("Ghost", "luts/ghost.cube", LookDefaults(grain = 30, fade = 55))
    }

fun LookPreset.toParams(intensity: Int = 100): LookParams = LookParams(
    preset = this,
    intensity = intensity,
    sharp = info.defaults.sharp,
    grain = info.defaults.grain,
    vignette = info.defaults.vignette,
    fade = info.defaults.fade,
)
