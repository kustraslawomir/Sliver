package app.sliver.data.look

import java.io.InputStream

object LutCube {
    /** Reads a .cube 3D LUT into r,g,b triples, red fastest (the file order). */
    fun parseTable(input: InputStream): FloatArray {
        var size = 0
        val data = ArrayList<Float>()
        input.bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                when {
                    line.isEmpty() || line.startsWith("#") -> Unit
                    line.startsWith("LUT_3D_SIZE") -> size = line.substringAfter("LUT_3D_SIZE").trim().toInt()
                    line[0].isLetter() -> Unit
                    else -> line.split(Regex("\\s+")).forEach { data += it.toFloat() }
                }
            }
        }
        require(size > 1 && data.size == size * size * size * 3) { "Invalid cube LUT" }
        return data.toFloatArray()
    }

    /** cube[r][g][b] packed ARGB, as expected by Media3 SingleColorLut; [intensity] blends with the identity LUT. */
    fun toCube(table: FloatArray, intensity: Float = 1f): Array<Array<IntArray>> {
        val size = Math.round(Math.cbrt((table.size / 3).toDouble())).toInt()
        val mix = intensity.coerceIn(0f, 1f)
        val last = (size - 1).toFloat()
        fun ch(lut: Float, identity: Float) = Math.round((identity + (lut - identity) * mix).coerceIn(0f, 1f) * 255f)
        return Array(size) { r ->
            Array(size) { g ->
                IntArray(size) { b ->
                    val i = (r + g * size + b * size * size) * 3
                    (0xFF shl 24) or (ch(table[i], r / last) shl 16) or (ch(table[i + 1], g / last) shl 8) or ch(table[i + 2], b / last)
                }
            }
        }
    }

    fun parse(input: InputStream, intensity: Float = 1f): Array<Array<IntArray>> = toCube(parseTable(input), intensity)
}
