package app.sliver.data.look

import android.content.Context
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.SingleColorLut
import app.sliver.data.LookParams
import app.sliver.data.info

/** Builds the Media3 effect chain for a [LookParams]; the same chain drives preview and export. */
@UnstableApi
object LookEffects {
    /**
     * [animateGrain] should be true only while playing (preview); exports always animate the grain.
     * Intensity blends the LUT with identity on the CPU, so no extra shader pass is needed.
     */
    fun build(context: Context, params: LookParams, forExport: Boolean, animateGrain: Boolean = true): List<Effect> {
        val effects = ArrayList<Effect>()
        val lut = params.preset.info.lutFile
        if (lut != null && params.intensity > 0) {
            val cube = context.assets.open(lut).use { LutCube.parse(it, params.intensity / 100f) }
            effects += SingleColorLut.createFromCube(cube)
        }
        if (params.contrast != 0) effects += Contrast(params.contrast / 100f)
        if (params.saturation != 0) {
            effects += HslAdjustment.Builder().adjustSaturation(params.saturation.toFloat()).build()
        }
        if (params.sharp > 0 || params.grain > 0 || params.vignette > 0 || params.fade > 0) {
            effects += LookShaderEffect(
                params.sharp / 100f, params.grain / 100f, params.vignette / 100f, params.fade / 100f,
                animateGrain = forExport || animateGrain,
            )
        }
        return effects
    }
}
