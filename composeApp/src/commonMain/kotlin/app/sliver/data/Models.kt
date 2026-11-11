package app.sliver.data

import kotlinx.serialization.Serializable

enum class DaySource { CAMERA, GALLERY, PHOTO, BACKFILL }
enum class LookPreset { NATURAL, NOIR, BLACKOUT, NIGHT_NEON, COLD_STEEL, EMBER, VHS_94, HARD_LIGHT, GHOST }
enum class Motion { STILL, ZOOM }
enum class BackupState { PENDING, DONE, FAILED }
enum class FilmFormat { CINEMA, STORY }
enum class FilmRangeType { WEEK, MONTH, YEAR, CUSTOM }
enum class FilmAudio { ORIGINAL, MUSIC, MIX }
enum class FilmCut { CUT, FADE }
enum class Mismatch { CENTER, BLUR }

@Serializable
data class LookParams(
    val preset: LookPreset = LookPreset.NATURAL,
    val intensity: Int = 100,
    val sharp: Int = 0,
    val contrast: Int = 0,
    val grain: Int = 0,
    val vignette: Int = 0,
    val fade: Int = 0,
    val saturation: Int = 0,
)
