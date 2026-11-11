package app.sliver.feature.films

import app.sliver.data.FilmAudio
import app.sliver.data.FilmCut
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

sealed interface RenderState {
    data object Idle : RenderState
    data class Running(val percent: Int) : RenderState
    data object Failed : RenderState
}

/** Renders a film to `films/<id>.mp4` in the background, surviving app close. */
interface FilmRenderer {
    fun start(filmId: String)
    fun cancel(filmId: String)
    fun state(filmId: String): Flow<RenderState>
}

const val FILM_FRAME_MS = 1000L
const val FILM_DIP_MS = 120L
const val FILM_TITLE_MS = 2000L
const val MIX_MUSIC_VOLUME = 0.3f

/** Brightness multiplier of a day clip at [positionMs]: a 120 ms dip to black at both boundaries for fade cuts. */
fun dipFactor(cut: FilmCut, positionMs: Long, clipMs: Long = FILM_FRAME_MS): Float =
    fadeAlpha(cut, (positionMs.toFloat() / clipMs).coerceIn(0f, 1f), FILM_DIP_MS.toFloat() / clipMs).let { 1f - it }

class AudioPlan(val keepClipAudio: Boolean, val music: Boolean, val musicVolume: Float)

/** ORIGINAL = clip audio; MUSIC = clips muted + track; MIX = clips 100% + music 30%. Without a track it falls back to ORIGINAL. */
fun audioPlan(audio: FilmAudio, track: String?): AudioPlan = when {
    track == null || audio == FilmAudio.ORIGINAL -> AudioPlan(true, false, 0f)
    audio == FilmAudio.MUSIC -> AudioPlan(false, true, 1f)
    else -> AudioPlan(true, true, MIX_MUSIC_VOLUME)
}

private val ShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun dateStamp(date: LocalDate): String = "${date.dayOfMonth} ${ShortMonths[date.monthNumber - 1]} ${date.year}"

/** Width and height fractions (0..1] of the output frame that a clip of [clipAspect] occupies when fitted inside it. */
fun fitScale(clipAspect: Float, outAspect: Float): Pair<Float, Float> =
    if (clipAspect > outAspect) 1f to outAspect / clipAspect else clipAspect / outAspect to 1f

/** True when a clip fills the output without visible bars or cropping worth handling. */
fun aspectMatches(clipAspect: Float, outAspect: Float): Boolean = kotlin.math.abs(clipAspect / outAspect - 1f) < 0.02f
