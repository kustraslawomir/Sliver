package app.sliver.feature.films

import app.sliver.data.FilmAudio
import app.sliver.data.FilmCut
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.LookPreset
import app.sliver.data.db.Film
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

const val MAX_DAY_TICKS = 40

class BuiltInTrack(val id: String, val label: String) {
    val asset: String get() = "music/$id.aac"
}

val BuiltInTracks = listOf(
    BuiltInTrack("drift", "Drift"),
    BuiltInTrack("pulse", "Pulse"),
    BuiltInTrack("dusk", "Dusk"),
    BuiltInTrack("spark", "Spark"),
)

const val ASSET_PREFIX = "asset:"

fun builtInTrackRef(track: BuiltInTrack) = ASSET_PREFIX + track.asset

fun trackLabel(ref: String?): String? = when {
    ref == null -> null
    ref.startsWith(ASSET_PREFIX) -> BuiltInTracks.firstOrNull { builtInTrackRef(it) == ref }?.label
    else -> "Your file"
}

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

/** Opacity of the black dip near the clip edges for fade cuts; [clipFraction] is progress through one second. */
fun fadeAlpha(cut: FilmCut, clipFraction: Float, edge: Float = 0.2f): Float {
    if (cut != FilmCut.FADE) return 0f
    return when {
        clipFraction < edge -> 1f - clipFraction / edge
        clipFraction > 1f - edge -> (clipFraction - (1f - edge)) / edge
        else -> 0f
    }.coerceIn(0f, 1f)
}

data class FilmOptions(
    val rangeType: FilmRangeType = FilmRangeType.YEAR,
    val custom: Pair<LocalDate, LocalDate>,
    val audio: FilmAudio = FilmAudio.ORIGINAL,
    val track: String? = null,
    val cut: FilmCut = FilmCut.CUT,
    val dateOn: Boolean = true,
    val look: LookPreset? = null,
)

fun FilmOptions.toFilm(id: String, name: String, format: FilmFormat, today: LocalDate): Film {
    val (from, to) = rangeFor(rangeType, today, custom)
    return Film(
        id = id, name = name, format = format, rangeType = rangeType, rangeFrom = from, rangeTo = to,
        audio = audio, track = track.takeIf { audio != FilmAudio.ORIGINAL }, cut = cut, dateOn = dateOn, look = look,
    )
}
