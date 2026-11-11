package app.sliver.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

data class LutRecipe(
    val gray: Double = 0.0,
    val contrast: Double = 1.0,
    val brightness: Double = 1.0,
    val sat: Double = 1.0,
    val sepia: Double = 0.0,
    val hue: Double = 0.0,
    val tintHex: String? = null,
    val tintAmount: Double = 0.0,
)

object LutGenerator {
    const val SIZE = 33

    val recipes: Map<String, LutRecipe> = linkedMapOf(
        "noir" to LutRecipe(gray = 1.0, contrast = 1.35, brightness = 0.92),
        "blackout" to LutRecipe(contrast = 1.6, brightness = 0.7, sat = 0.75),
        "night_neon" to LutRecipe(sat = 1.7, contrast = 1.2, brightness = 0.88, hue = -12.0, tintHex = "7A2AC8", tintAmount = 0.45),
        "cold_steel" to LutRecipe(sat = 0.45, contrast = 1.18, tintHex = "3A8AC8", tintAmount = 0.55),
        "ember" to LutRecipe(sepia = 0.3, sat = 1.35, contrast = 1.08, tintHex = "FF7A2A", tintAmount = 0.45),
        "vhs_94" to LutRecipe(sat = 1.35, contrast = 0.85, brightness = 1.05, tintHex = "B04AC8", tintAmount = 0.20),
        "hard_light" to LutRecipe(contrast = 1.65, brightness = 1.06, sat = 1.12),
        "ghost" to LutRecipe(gray = 0.65, brightness = 1.15, contrast = 0.72),
    )

    private val sepiaMatrix = arrayOf(
        doubleArrayOf(0.393, 0.769, 0.189),
        doubleArrayOf(0.349, 0.686, 0.168),
        doubleArrayOf(0.272, 0.534, 0.131),
    )

    private fun luma(c: DoubleArray) = 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]

    private fun clip(v: Double) = if (v < 0.0) 0.0 else if (v > 1.0) 1.0 else v

    private fun mul(m: Array<DoubleArray>, c: DoubleArray) =
        DoubleArray(3) { i -> m[i][0] * c[0] + m[i][1] * c[1] + m[i][2] * c[2] }

    private fun hueMatrix(deg: Double): Array<DoubleArray> {
        val a = deg * PI / 180.0
        val c = cos(a)
        val s = sin(a)
        return arrayOf(
            doubleArrayOf(0.213 + c * 0.787 - s * 0.213, 0.715 - c * 0.715 - s * 0.715, 0.072 - c * 0.072 + s * 0.928),
            doubleArrayOf(0.213 - c * 0.213 + s * 0.143, 0.715 + c * 0.285 + s * 0.140, 0.072 - c * 0.072 - s * 0.283),
            doubleArrayOf(0.213 - c * 0.213 - s * 0.787, 0.715 - c * 0.715 + s * 0.715, 0.072 + c * 0.928 + s * 0.072),
        )
    }

    private fun softLight(base: Double, blend: Double) =
        if (blend <= 0.5) base - (1 - 2 * blend) * base * (1 - base)
        else base + (2 * blend - 1) * (sqrt(base) - base)

    fun apply(rgb: DoubleArray, r: LutRecipe): DoubleArray {
        var c = rgb.copyOf()
        if (r.gray != 0.0) {
            val l = luma(c)
            c = DoubleArray(3) { c[it] * (1 - r.gray) + l * r.gray }
        }
        if (r.sepia != 0.0) {
            val s = mul(sepiaMatrix, c)
            c = DoubleArray(3) { c[it] * (1 - r.sepia) + clip(s[it]) * r.sepia }
        }
        if (r.hue != 0.0) c = mul(hueMatrix(r.hue), c)
        if (r.sat != 1.0) {
            val l = luma(c)
            c = DoubleArray(3) { l + (c[it] - l) * r.sat }
        }
        c = DoubleArray(3) { (clip(c[it]) - 0.5) * r.contrast + 0.5 }
        c = DoubleArray(3) { clip(c[it] * r.brightness) }
        if (r.tintHex != null) {
            val t = DoubleArray(3) { r.tintHex.substring(it * 2, it * 2 + 2).toInt(16) / 255.0 }
            c = DoubleArray(3) { c[it] * (1 - r.tintAmount) + softLight(c[it], t[it]) * r.tintAmount }
        }
        return DoubleArray(3) { clip(c[it]) }
    }

    private fun fmt(v: Double): String {
        val n = (v * 1_000_000).roundToLong()
        return "${n / 1_000_000}.${(n % 1_000_000).toString().padStart(6, '0')}"
    }

    fun cube(name: String, recipe: LutRecipe): String = buildString {
        append("TITLE \"$name\"\nLUT_3D_SIZE $SIZE\nDOMAIN_MIN 0 0 0\nDOMAIN_MAX 1 1 1\n")
        // .cube order: red fastest, then green, then blue
        for (b in 0 until SIZE) for (g in 0 until SIZE) for (r in 0 until SIZE) {
            val o = apply(doubleArrayOf(r / (SIZE - 1.0), g / (SIZE - 1.0), b / (SIZE - 1.0)), recipe)
            append(fmt(o[0])).append(' ').append(fmt(o[1])).append(' ').append(fmt(o[2])).append('\n')
        }
    }

    fun all(): Map<String, String> = recipes.mapValues { (n, r) -> cube(n, r) }
}
