package app.sliver.feature.films

import app.sliver.core.APP_NAME

import kotlin.math.pow

const val PREMIERE_SLIT_MS = 1000L
const val PREMIERE_TITLE_DELAY_MS = 1400L
const val PREMIERE_TITLE_FADE_MS = 600L
const val PREMIERE_END_MS = PREMIERE_TITLE_DELAY_MS + PREMIERE_TITLE_FADE_MS + 1000L

/** Height fraction of the film frame the slit has opened to at [ms]: ease-out cubic over 1000 ms. */
fun slitFraction(ms: Long): Float {
    val t = (ms.toFloat() / PREMIERE_SLIT_MS).coerceIn(0f, 1f)
    return 1f - (1f - t).pow(3)
}

fun titleAlpha(ms: Long): Float = ((ms - PREMIERE_TITLE_DELAY_MS).toFloat() / PREMIERE_TITLE_FADE_MS).coerceIn(0f, 1f)

/** The rendered film is the days (one second each) followed by the title card. */
fun dayIndexAtMs(positionMs: Long, dayCount: Int): Int =
    if (dayCount == 0) 0 else (positionMs / FILM_FRAME_MS).toInt().coerceIn(0, dayCount - 1)

/** Scrubber tick positions as fractions of the full rendered duration (days plus title card). */
fun playerTicks(plan: FilmPlan, durationMs: Long): List<Float> {
    val total = durationMs.takeIf { it > 0 } ?: (plan.clips * FILM_FRAME_MS + FILM_TITLE_MS)
    return scrubberTicks(plan).map { it * plan.clips * FILM_FRAME_MS / total }
}

/** Holds the film the export, premiere and player screens work on (routes carry no arguments). */
object FilmNav {
    var id: String? = null

    /** Film the builder should open for editing; consumed once by the builder. */
    var editId: String? = null

    fun takeEditId(): String? = editId.also { editId = null }
}

interface FilmShare {
    fun share(path: String, name: String)

    /** Copies the film into the shared Movies collection; true when it was saved. */
    suspend fun saveToPhotos(path: String, name: String): Boolean

    fun sizeBytes(path: String): Long
}

/** One-line file info under the player actions (design/src/FilmPlayer.dc.html). Both formats render at 1080p. */
fun filmInfoLine(sizeBytes: Long): String {
    val mb = ((sizeBytes + 500_000) / 1_000_000).coerceAtLeast(1)
    return "MP4 · 1080p · 30 fps · $mb MB · kept in $APP_NAME"
}
