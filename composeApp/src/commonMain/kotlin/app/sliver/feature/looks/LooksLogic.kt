package app.sliver.feature.looks

import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.export.ShortMode
import app.sliver.data.LookPreset
import app.sliver.data.toParams
import kotlinx.datetime.LocalDate

enum class AdjustTool(val label: String, val bipolar: Boolean = false) {
    SHARPNESS("Sharpness"),
    CONTRAST("Contrast", bipolar = true),
    GRAIN("Grain"),
    VIGNETTE("Vignette"),
    FADE("Fade"),
    SATURATION("Saturation", bipolar = true),
}

fun LookParams.value(tool: AdjustTool): Int = when (tool) {
    AdjustTool.SHARPNESS -> sharp
    AdjustTool.CONTRAST -> contrast
    AdjustTool.GRAIN -> grain
    AdjustTool.VIGNETTE -> vignette
    AdjustTool.FADE -> fade
    AdjustTool.SATURATION -> saturation
}

fun LookParams.with(tool: AdjustTool, value: Int): LookParams {
    val v = value.coerceIn(if (tool.bipolar) -100 else 0, 100)
    return when (tool) {
        AdjustTool.SHARPNESS -> copy(sharp = v)
        AdjustTool.CONTRAST -> copy(contrast = v)
        AdjustTool.GRAIN -> copy(grain = v)
        AdjustTool.VIGNETTE -> copy(vignette = v)
        AdjustTool.FADE -> copy(fade = v)
        AdjustTool.SATURATION -> copy(saturation = v)
    }
}

/** Resets a tool to what the current preset ships with. */
fun LookParams.reset(tool: AdjustTool): LookParams = with(tool, preset.toParams(intensity).value(tool))

fun LookParams.resetAll(): LookParams = preset.toParams(intensity)

fun LookParams.isModified(tool: AdjustTool): Boolean = value(tool) != reset(tool).value(tool)

/** Slider 0..1 <-> tool value. */
fun AdjustTool.toFraction(value: Int): Float = if (bipolar) (value + 100) / 200f else value / 100f

fun AdjustTool.fromFraction(f: Float): Int =
    (if (bipolar) f * 200f - 100f else f * 100f).let { kotlin.math.round(it).toInt() }

fun LookParams.withPreset(next: LookPreset): LookParams = next.toParams(intensity)

/** What the Trim / Photo screens hand to the editor and take back; in-memory like CaptureSession. */
object LookSession {
    var isPhoto = false
    var startMs = 0L
    var windowMs = 1000L
    var speed = 1f
    var params = LookParams()

    /** Set by the editor on Apply, consumed once by the caller. */
    var applied: LookParams? = null

    var resume: Resume? = null

    fun takeApplied(): LookParams? = applied.also { applied = null }
    fun takeResume(): Resume? = resume.also { resume = null }

    data class Resume(
        val startMs: Long = 0,
        val custom: Boolean = false,
        val speed: Float = 1f,
        val audio: Boolean = true,
        val shortMode: ShortMode = ShortMode.SLOW_TO_FIT,
        val day: LocalDate? = null,
        val motion: Motion = Motion.ZOOM,
    )
}
