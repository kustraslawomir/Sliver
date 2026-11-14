package app.sliver.data

import kotlinx.serialization.Serializable

enum class DaySource { CAMERA, GALLERY, PHOTO, BACKFILL }
enum class LookPreset { NATURAL, KIOSK, AFTERHOURS, SILVER, SUNDAY, TAPE, HALO, C02, C03, C04, C05, C07, C08, C09, C10, C11, C12, C13, C14, C15, C16, C17, C18, C19, C20, C21, C22, C24, C25, C26, C27, C28, C30, C31, C32, C35, C36, C37, C38, C39, C40, C41, C42, C43, C44, C45, C46, C47, C48, C49, C50, C51, C52, C53, C54 }
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
