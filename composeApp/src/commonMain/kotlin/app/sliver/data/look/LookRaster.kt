package app.sliver.data.look

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CPU port of the one-pass GLSL look shader (androidMain `LookShader`). Used for filter thumbnails in the Film builder
 * and for the contact sheets. Same maths and constants; differences: bilinear taps are written by hand, the grain hash
 * runs in double precision, and the LUT is a trilinear lookup into the same 33^3 table (the GPU uses Media3's LUT).
 */
object LookRaster {
    private const val TAPS = 12
    private val GOLDEN = 2.39996323f

    /** Renders [src] (ARGB, [w] x [h]) with the colour [lut] blended by [lutMix] and the shader [f]. */
    fun render(src: IntArray, w: Int, h: Int, lut: FloatArray?, lutMix: Float, f: LookFrame): IntArray {
        val px = FloatArray(w * h * 3)
        val tmp = FloatArray(3)
        for (i in src.indices) {
            var r = ((src[i] shr 16) and 255) / 255f
            var g = ((src[i] shr 8) and 255) / 255f
            var b = (src[i] and 255) / 255f
            if (lut != null && lutMix > 0f) {
                LutGenerator.sample(lut, r, g, b, tmp)
                r += (tmp[0] - r) * lutMix; g += (tmp[1] - g) * lutMix; b += (tmp[2] - b) * lutMix
            }
            px[i * 3] = r; px[i * 3 + 1] = g; px[i * 3 + 2] = b
        }
        val out = IntArray(w * h)
        if (f.isIdentity) {
            for (i in out.indices) out[i] = pack(px[i * 3], px[i * 3 + 1], px[i * 3 + 2])
            return out
        }
        val s = Sampler(px, w, h)
        val c = FloatArray(3)
        val t = FloatArray(3)
        val aspect = w.toFloat() / h
        val corner = sqrt(aspect * aspect + 1f) * 0.5f
        val texX = 1f / w
        val texY = 1f / h
        val goldenCos = FloatArray(TAPS) { cos(it * GOLDEN) }
        val goldenSin = FloatArray(TAPS) { sin(it * GOLDEN) }
        val goldenRad = FloatArray(TAPS) { sqrt((it + 0.5f) / TAPS) }
        for (y in 0 until h) {
            for (x in 0 until w) {
                val ux = (x + 0.5f) * texX + f.weaveDx * texX
                val uy = (y + 0.5f) * texY + f.weaveDy * texY
                val pX = ux - 0.5f
                val pY = uy - 0.5f
                val rn = sqrt(pX * pX * aspect * aspect + pY * pY) / corner
                val rn2 = rn * rn
                val barrel = 1f - 0.06f * f.soft * rn2
                val ca = 0.010f * f.soft * rn2
                s.at(0.5f + pX * barrel * (1 + ca), 0.5f + pY * barrel * (1 + ca), t); val cr = t[0]
                s.at(0.5f + pX * barrel, 0.5f + pY * barrel, t); val cg = t[1]
                s.at(0.5f + pX * barrel * (1 - ca), 0.5f + pY * barrel * (1 - ca), t); val cb = t[2]
                c[0] = cr; c[1] = cg; c[2] = cb
                val gx = 0.5f + pX * barrel
                val gy = 0.5f + pY * barrel
                if (f.soft > 0f) {
                    val eb = f.soft * smoothstep(0.35f, 1f, rn)
                    if (eb > 0f) {
                        val rad = 3f * eb
                        var ar = 0f; var ag = 0f; var ab = 0f
                        for (d in 0 until 4) {
                            val sx = if (d and 1 == 0) -1f else 1f
                            val sy = if (d and 2 == 0) -1f else 1f
                            s.at(gx + sx * rad * texX, gy + sy * rad * texY, t)
                            ar += t[0]; ag += t[1]; ab += t[2]
                        }
                        val m = min(1f, eb * 0.9f)
                        c[0] += (ar / 4 - c[0]) * m; c[1] += (ag / 4 - c[1]) * m; c[2] += (ab / 4 - c[2]) * m
                    }
                }
                if (f.bleed > 0f) {
                    var ar = 0f; var ag = 0f; var ab = 0f
                    for (d in 0 until 4) {
                        val off = (if (d < 2) -1f else 1f) * (if (d % 2 == 0) 4f else 9f) * texX * (h / 360f).coerceAtLeast(1f)
                        s.at(ux + off, uy, t)
                        ar += t[0]; ag += t[1]; ab += t[2]
                    }
                    ar /= 4; ag /= 4; ab /= 4
                    val l = luma(c[0], c[1], c[2])
                    val ls = luma(ar, ag, ab)
                    val m = f.bleed * 0.8f
                    c[0] = l + (c[0] - l) * (1 - m) + (ar - ls) * m
                    c[1] = l + (c[1] - l) * (1 - m) + (ag - ls) * m
                    c[2] = l + (c[2] - l) * (1 - m) + (ab - ls) * m
                }
                if (f.halation > 0f) {
                    var hr = 0f; var hg = 0f; var hb = 0f
                    for (i in 0 until TAPS) {
                        s.at(ux + goldenCos[i] * goldenRad[i] * 0.012f * h * texX, uy + goldenSin[i] * goldenRad[i] * 0.012f, t)
                        val k = smoothstep(0.65f, 1f, luma(t[0], t[1], t[2]))
                        hr += t[0] * k; hg += t[1] * k; hb += t[2] * k
                    }
                    val hl = luma(hr, hg, hb) / TAPS
                    c[0] += f.halation * 1.6f * hl; c[1] += f.halation * 1.6f * hl * 0.38f; c[2] += f.halation * 1.6f * hl * 0.14f
                }
                if (f.bloom > 0f) {
                    var hr = 0f; var hg = 0f; var hb = 0f
                    var ar = 0f; var ag = 0f; var ab = 0f
                    for (i in 0 until TAPS) {
                        s.at(ux + goldenCos[i] * goldenRad[i] * 0.03f * h * texX, uy + goldenSin[i] * goldenRad[i] * 0.03f, t)
                        val k = smoothstep(0.55f, 1f, luma(t[0], t[1], t[2]))
                        hr += t[0] * k; hg += t[1] * k; hb += t[2] * k
                        ar += t[0]; ag += t[1]; ab += t[2]
                    }
                    val m = f.bloom * 0.25f
                    c[0] = c[0] + (ar / TAPS - c[0]) * m + f.bloom * 0.9f * hr / TAPS
                    c[1] = c[1] + (ag / TAPS - c[1]) * m + f.bloom * 0.9f * hg / TAPS
                    c[2] = c[2] + (ab / TAPS - c[2]) * m + f.bloom * 0.9f * hb / TAPS
                }
                if (f.flash > 0f) {
                    val fall = 1f - smoothstep(0f, 0.95f, rn)
                    val gain = 1f + f.flash * (0.5f * fall - 0.28f * (1f - fall))
                    val warm = f.flash * fall
                    c[0] *= gain * (1f + 0.10f * warm); c[1] *= gain; c[2] *= gain * (1f - 0.14f * warm)
                }
                c[0] *= f.exposure; c[1] *= f.exposure; c[2] *= f.exposure
                if (f.leak.strength > 0f) {
                    val dx = (ux - f.leak.x) * aspect
                    val dy = uy - f.leak.y
                    val lk = f.leak.strength * smoothstep(f.leak.radius, 0f, sqrt(dx * dx + dy * dy))
                    c[0] = 1f - (1f - c[0]) * (1f - f.leak.r * lk)
                    c[1] = 1f - (1f - c[1]) * (1f - f.leak.g * lk)
                    c[2] = 1f - (1f - c[2]) * (1f - f.leak.b * lk)
                }
                if (f.scan > 0f) {
                    val m = 0.5f + 0.5f * cos(2f * PI.toFloat() * y / f.scanPeriod)
                    val k = 1f - f.scan * 0.22f * m
                    c[0] *= k; c[1] *= k; c[2] *= k
                }
                if (f.fade > 0f) {
                    for (q in 0 until 3) c[q] = c[q] * (1f - 0.25f * f.fade) + 0.12f * f.fade
                }
                if (f.vignette > 0f) {
                    val k = 1f - f.vignette * smoothstep(0.35f, 1f, rn)
                    c[0] *= k; c[1] *= k; c[2] *= k
                }
                if (f.grain > 0f) {
                    val cx = floor(x / f.grainPx)
                    val cy = floor(y / f.grainPx)
                    val l = luma(c[0], c[1], c[2])
                    val wgt = 1f - 0.75f * smoothstep(0.45f, 1f, l)
                    val n0 = rand(cx, cy, f.grainSeed, 0f) - 0.5f
                    val gr = n0 * (1 - f.grainChroma) + (rand(cx, cy, f.grainSeed, 1.7f) - 0.5f) * f.grainChroma
                    val gg = n0 * (1 - f.grainChroma) + (rand(cx, cy, f.grainSeed, 3.1f) - 0.5f) * f.grainChroma
                    val gb = n0 * (1 - f.grainChroma) + (rand(cx, cy, f.grainSeed, 5.3f) - 0.5f) * f.grainChroma
                    val a = f.grain * 0.35f * wgt
                    c[0] += gr * a; c[1] += gg * a; c[2] += gb * a
                }
                out[y * w + x] = pack(c[0], c[1], c[2])
            }
        }
        return out
    }

    private fun rand(cx: Float, cy: Float, seed: Float, salt: Float): Float {
        val v = sin((cx + salt) * 12.9898 + (cy + seed * 0.37) * 78.233) * 43758.5453
        return (v - floor(v)).toFloat()
    }

    private fun luma(r: Float, g: Float, b: Float) = 0.2126f * r + 0.7152f * g + 0.0722f * b

    private fun smoothstep(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun pack(r: Float, g: Float, b: Float): Int {
        fun q(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (0xFF shl 24) or (q(r) shl 16) or (q(g) shl 8) or q(b)
    }

    private class Sampler(val px: FloatArray, val w: Int, val h: Int) {
        /** Bilinear, clamp-to-edge, uv in 0..1. */
        fun at(u: Float, v: Float, out: FloatArray) {
            val fx = (u * w - 0.5f).coerceIn(0f, (w - 1).toFloat())
            val fy = (v * h - 0.5f).coerceIn(0f, (h - 1).toFloat())
            val x0 = fx.toInt(); val y0 = fy.toInt()
            val x1 = min(x0 + 1, w - 1); val y1 = min(y0 + 1, h - 1)
            val tx = fx - x0; val ty = fy - y0
            for (c in 0 until 3) {
                val a = px[(y0 * w + x0) * 3 + c] * (1 - tx) + px[(y0 * w + x1) * 3 + c] * tx
                val b = px[(y1 * w + x0) * 3 + c] * (1 - tx) + px[(y1 * w + x1) * 3 + c] * tx
                out[c] = a * (1 - ty) + b * ty
            }
        }
    }
}
