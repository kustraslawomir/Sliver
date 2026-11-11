package app.sliver.data.look

import java.io.InputStream

object LutCube {
    /** Parses a .cube 3D LUT into cube[r][g][b] packed ARGB, as expected by Media3 SingleColorLut. */
    fun parse(input: InputStream, intensity: Float = 1f): Array<Array<IntArray>> {
        var size = 0
        val data = ArrayList<FloatArray>()
        input.bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                when {
                    line.isEmpty() || line.startsWith("#") -> Unit
                    line.startsWith("LUT_3D_SIZE") -> size = line.substringAfter("LUT_3D_SIZE").trim().toInt()
                    line[0].isLetter() -> Unit
                    else -> line.split(Regex("\\s+")).let { data += floatArrayOf(it[0].toFloat(), it[1].toFloat(), it[2].toFloat()) }
                }
            }
        }
        require(size > 1 && data.size == size * size * size) { "Invalid cube LUT" }
        val mix = intensity.coerceIn(0f, 1f)
        val last = (size - 1).toFloat()
        fun ch(lut: Float, identity: Float) = Math.round((identity + (lut - identity) * mix).coerceIn(0f, 1f) * 255f)
        return Array(size) { r ->
            Array(size) { g ->
                IntArray(size) { b ->
                    val v = data[r + g * size + b * size * size]
                    (0xFF shl 24) or (ch(v[0], r / last) shl 16) or (ch(v[1], g / last) shl 8) or ch(v[2], b / last)
                }
            }
        }
    }
}
