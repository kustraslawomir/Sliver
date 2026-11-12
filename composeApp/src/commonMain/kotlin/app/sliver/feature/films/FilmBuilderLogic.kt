package app.sliver.feature.films

import app.sliver.data.FilmAudio
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.LookPreset
import app.sliver.data.db.Film
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

const val MAX_DAY_TICKS = 40

/** Tracks of films made before music files had to be picked ("asset:music/…") no longer exist. */
const val LEGACY_ASSET_PREFIX = "asset:"

const val LEGACY_TRACK_NOTE = "The built-in music was removed. This film now uses its original sound."
const val PICK_MUSIC_HINT = "Pick a music file to save"

/** The track a film can really play, or null for removed built-in tracks. */
fun usableTrack(ref: String?): String? = ref?.takeIf { !it.startsWith(LEGACY_ASSET_PREFIX) }

fun isLegacyTrack(ref: String?): Boolean = ref != null && usableTrack(ref) == null

/** File name shown for a picked track; copies are stored as "<millis>_<name>". */
fun trackLabel(ref: String?): String? = usableTrack(ref)?.substringAfterLast('/')?.replace(Regex("^\\d+_"), "")

/** Music and Mix need a picked file; Original never does. */
fun canSaveSound(audio: FilmAudio, track: String?): Boolean = audio == FilmAudio.ORIGINAL || usableTrack(track) != null

fun rangeFor(type: FilmRangeType, today: LocalDate, custom: Pair<LocalDate, LocalDate>): Pair<LocalDate, LocalDate> =
    when (type) {
        FilmRangeType.WEEK -> today.plus(-6, DateTimeUnit.DAY) to today
        FilmRangeType.MONTH -> {
            val first = LocalDate(today.year, today.monthNumber, 1)
            first to first.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
        }
        FilmRangeType.YEAR -> LocalDate(today.year, 1, 1) to LocalDate(today.year, 12, 31)
        FilmRangeType.CUSTOM -> custom
    }

/** Fractions (0..1) of the film where scrubber ticks sit: one per day, or one per month start above [MAX_DAY_TICKS]. */
fun scrubberTicks(plan: FilmPlan): List<Float> {
    val n = plan.days.size
    if (n == 0) return emptyList()
    if (n <= MAX_DAY_TICKS) return List(n) { it / n.toFloat() }
    return plan.days.indices.filter { it == 0 || plan.days[it].date.monthNumber != plan.days[it - 1].date.monthNumber }
        .map { it / n.toFloat() }
}

/** Index of the day under [fraction] of the film. */
fun dayIndexAt(fraction: Float, count: Int): Int =
    if (count == 0) 0 else (fraction.coerceIn(0f, 1f) * count).toInt().coerceAtMost(count - 1)

data class FilmOptions(
    val rangeType: FilmRangeType = FilmRangeType.YEAR,
    val custom: Pair<LocalDate, LocalDate>,
    val audio: FilmAudio = FilmAudio.ORIGINAL,
    val track: String? = null,
    val dateOn: Boolean = true,
    val look: LookPreset? = null,
)

fun FilmOptions.toFilm(id: String, name: String, format: FilmFormat, today: LocalDate): Film {
    val (from, to) = rangeFor(rangeType, today, custom)
    return Film(
        id = id, name = name, format = format, rangeType = rangeType, rangeFrom = from, rangeTo = to,
        audio = audio, track = usableTrack(track).takeIf { audio != FilmAudio.ORIGINAL }, dateOn = dateOn, look = look,
    )
}
