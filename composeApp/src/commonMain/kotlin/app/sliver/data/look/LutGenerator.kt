package app.sliver.data.look

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** Builds 33^3 colour LUTs from a [LookModel]'s colour part. */
object LutGenerator {
    const val SIZE = 33

    private fun clip(v: Double) = if (v < 0.0) 0.0 else if (v > 1.0) 1.0 else v
    private fun luma(r: Double, g: Double, b: Double) = 0.2126 * r + 0.7152 * g + 0.0722 * b
    private fun smooth(x: Double) = x * x * (3 - 2 * x)

    private fun bandFactor(bands: List<Double>, hue: Double): Double {
        val a = (hue / 60.0).toInt() % 6
        val t = hue / 60.0 - (hue / 60.0).toInt()
        return bands[a] * (1 - t) + bands[(a + 1) % 6] * t
    }

    fun apply(rgb: DoubleArray, m: LookModel): DoubleArray {
        var r = rgb[0];
        var g = rgb[1];
        var b = rgb[2]
        if (m.exposure != 0.0) {
            val k = 2.0.pow(m.exposure);
            r *= k;
            g *= k;
            b *= k
        }
        if (m.temp != 0.0 || m.tint != 0.0) {
            r *= 1 + 0.18 * m.temp + 0.06 * m.tint
            g *= 1 - 0.12 * m.tint + 0.02 * m.temp
            b *= 1 - 0.18 * m.temp + 0.06 * m.tint
        }
        r = clip(r);
        g = clip(g);
        b = clip(b)
        if (m.mono.size == 3) {
            val l = clip(m.mono[0] * r + m.mono[1] * g + m.mono[2] * b)
            r = l;
            g = l;
            b = l
        }
        if (m.gamma.size == 3) {
            r = r.pow(m.gamma[0]);
            g = g.pow(m.gamma[1]);
            b = b.pow(m.gamma[2])
        }
        if (m.contrast != 0.0) {
            val k = m.contrast
            fun curve(c: Double) = if (k >= 0) c + k * (smooth(c) - c) else 0.5 + (c - 0.5) * (1 + 0.5 * k)
            r = curve(r);
            g = curve(g);
            b = curve(b)
        }
        if (m.shoulder != 0.0) {
            fun roll(c: Double): Double {
                val x = max(0.0, c - 0.7) / 0.3
                return c - m.shoulder * 0.14 * x * x
            }
            r = roll(r);
            g = roll(g);
            b = roll(b)
        }
        if (m.saturation != 1.0 || m.bands.size == 6) {
            val l = luma(r, g, b)
            val mx = max(r, max(g, b));
            val mn = min(r, min(g, b));
            val d = mx - mn
            var k = m.saturation
            if (m.bands.size == 6 && d > 1e-6) {
                var h = when (mx) {
                    r -> ((g - b) / d) % 6.0
                    g -> (b - r) / d + 2.0
                    else -> (r - g) / d + 4.0
                } * 60.0
                if (h < 0) h += 360.0
                k *= bandFactor(m.bands, h)
            }
            r = l + (r - l) * k;
            g = l + (g - l) * k;
            b = l + (b - l) * k
        }
        val l = luma(clip(r), clip(g), clip(b))
        if (m.shadowTint.size == 3) {
            val w = (1 - l) * (1 - l)
            r += m.shadowTint[0] * w;
            g += m.shadowTint[1] * w;
            b += m.shadowTint[2] * w
        }
        if (m.highlightTint.size == 3) {
            val w = l * l
            r += m.highlightTint[0] * w;
            g += m.highlightTint[1] * w;
            b += m.highlightTint[2] * w
        }
        if (m.blackLift != 0.0) {
            val k = m.blackLift
            r = k + clip(r) * (1 - k);
            g = k + clip(g) * (1 - k);
            b = k + clip(b) * (1 - k)
        }
        return doubleArrayOf(clip(r), clip(g), clip(b))
    }

    /** Lattice values, red fastest, then green, then blue (the .cube order), as r,g,b triples. */
    fun table(m: LookModel): FloatArray {
        val out = FloatArray(SIZE * SIZE * SIZE * 3)
        var i = 0
        for (b in 0 until SIZE) for (g in 0 until SIZE) for (r in 0 until SIZE) {
            val o = apply(doubleArrayOf(r / (SIZE - 1.0), g / (SIZE - 1.0), b / (SIZE - 1.0)), m)
            out[i++] = o[0].toFloat();
            out[i++] = o[1].toFloat();
            out[i++] = o[2].toFloat()
        }
        return out
    }

    private fun fmt(v: Float): String {
        val n = (v.toDouble() * 1_000_000).roundToLong()
        return "${n / 1_000_000}.${(n % 1_000_000).toString().padStart(6, '0')}"
    }

    fun cube(name: String, m: LookModel): String = buildString {
        append("TITLE \"$name\"\nLUT_3D_SIZE $SIZE\nDOMAIN_MIN 0 0 0\nDOMAIN_MAX 1 1 1\n")
        val t = table(m)
        for (i in t.indices step 3) append(fmt(t[i])).append(' ').append(fmt(t[i + 1])).append(' ').append(fmt(t[i + 2])).append('\n')
    }

    /** Parses [cube] text back to a table (same layout as [table]). */
    fun parseCube(text: String): FloatArray {
        val out = ArrayList<Float>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line[0].isLetter() || line[0] == '#') continue
            line.split(' ').forEach { out += it.toFloat() }
        }
        return out.toFloatArray()
    }

    /** Trilinear lookup into a [table]. */
    fun sample(table: FloatArray, r: Float, g: Float, b: Float, out: FloatArray) {
        val s = (SIZE - 1).toFloat()
        val fr = r.coerceIn(0f, 1f) * s;
        val fg = g.coerceIn(0f, 1f) * s;
        val fb = b.coerceIn(0f, 1f) * s
        val r0 = min(fr.toInt(), SIZE - 2);
        val g0 = min(fg.toInt(), SIZE - 2);
        val b0 = min(fb.toInt(), SIZE - 2)
        val tr = fr - r0;
        val tg = fg - g0;
        val tb = fb - b0
        for (c in 0 until 3) {
            fun at(dr: Int, dg: Int, db: Int) = table[((b0 + db) * SIZE * SIZE + (g0 + dg) * SIZE + (r0 + dr)) * 3 + c]
            val c00 = at(0, 0, 0) * (1 - tr) + at(1, 0, 0) * tr
            val c10 = at(0, 1, 0) * (1 - tr) + at(1, 1, 0) * tr
            val c01 = at(0, 0, 1) * (1 - tr) + at(1, 0, 1) * tr
            val c11 = at(0, 1, 1) * (1 - tr) + at(1, 1, 1) * tr
            out[c] = (c00 * (1 - tg) + c10 * tg) * (1 - tb) + (c01 * (1 - tg) + c11 * tg) * tb
        }
    }
}
