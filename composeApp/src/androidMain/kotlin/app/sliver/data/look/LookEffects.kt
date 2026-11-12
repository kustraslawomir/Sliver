package app.sliver.data.look

import android.content.Context
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.SingleColorLut
import app.sliver.data.LookParams
import app.sliver.data.info
import java.util.concurrent.ConcurrentHashMap

/**
 * Builds the Media3 effect chain for a film's filter; the same chain drives preview and export. Filters belong to the
 * film only: callers never pass a per-clip look, and with no film filter they do not call this at all.
 */
@UnstableApi
object LookEffects {
    private val tables = ConcurrentHashMap<String, FloatArray>()

    /**
     * [clipId] seeds the grain, weave and light leak so a clip looks the same in preview and export.
     * [animate] false (paused preview) freezes the per-frame randomness; exports always animate.
     * Intensity blends the LUT with identity on the CPU and scales the shader amounts.
     */
    fun build(context: Context, params: LookParams, clipId: String, forExport: Boolean, animate: Boolean = true): List<Effect> {
        val info = params.preset.info
        if (params.intensity <= 0 && params.grain <= 0) return emptyList()
        val effects = ArrayList<Effect>()
        val lut = info.lutFile
        if (lut != null && params.intensity > 0) {
            val table = tables.getOrPut(lut) { context.assets.open(lut).use { LutCube.parseTable(it) } }
            effects += SingleColorLut.createFromCube(LutCube.toCube(table, params.intensity / 100f))
        }
        effects += LookShaderEffect(info.model, params.intensity / 100f, params.grain / 100f, LookRandom.seedOf(clipId), forExport || animate)
        return effects
    }
}
