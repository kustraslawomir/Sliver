package app.sliver.data.look

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/**
 * The film-look pass: plastic lens (barrel, edge blur, chromatic aberration), chroma bleed, halation, bloom, flash falloff,
 * flicker, light leak, scanlines, fade, vignette and luminance-weighted film grain with gate weave, in one fragment shader.
 * Every per-frame number comes from [lookFrame], which is also what the CPU port [LookRaster] uses.
 * Presentation time drives the per-frame randomness; if the transformer hands out times that differ from the preview's,
 * grain and weave still look the same statistically but are not frame-identical.
 */
@UnstableApi
class LookShaderEffect(
    private val model: LookModel,
    private val intensity: Float,
    private val grain: Float,
    private val seed: Long,
    private val animate: Boolean,
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        LookShaderProgram(useHdr, model, intensity, grain, seed, animate)
}

@UnstableApi
private class LookShaderProgram(
    useHdr: Boolean,
    private val model: LookModel,
    private val intensity: Float,
    private val grain: Float,
    private val seed: Long,
    private val animate: Boolean,
) : BaseGlShaderProgram(useHdr, 1) {
    private val program = try {
        GlProgram(VERTEX, FRAGMENT)
    } catch (e: Exception) {
        throw VideoFrameProcessingException(e)
    }
    private var width = 1
    private var height = 1

    init {
        program.setBufferAttribute("aFramePosition", GlUtil.getNormalizedCoordinateBounds(), 4)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        width = inputWidth
        height = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            val f = lookFrame(model, intensity, grain, seed, presentationTimeUs, height, animate)
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatsUniform("uRes", floatArrayOf(width.toFloat(), height.toFloat()))
            program.setFloatUniform("uGrain", f.grain)
            program.setFloatUniform("uGrainPx", f.grainPx)
            program.setFloatUniform("uGrainChroma", f.grainChroma)
            program.setFloatUniform("uGrainSeed", f.grainSeed)
            program.setFloatUniform("uHalation", f.halation)
            program.setFloatUniform("uBloom", f.bloom)
            program.setFloatUniform("uVignette", f.vignette)
            program.setFloatUniform("uFlash", f.flash)
            program.setFloatUniform("uSoft", f.soft)
            program.setFloatUniform("uScan", f.scan)
            program.setFloatUniform("uScanPeriod", f.scanPeriod)
            program.setFloatUniform("uBleed", f.bleed)
            program.setFloatUniform("uFade", f.fade)
            program.setFloatsUniform("uWeave", floatArrayOf(f.weaveDx, -f.weaveDy))
            program.setFloatUniform("uExposure", f.exposure)
            // Leak y is stored top-down; GL uv is bottom-up.
            program.setFloatsUniform("uLeak", floatArrayOf(f.leak.strength, f.leak.x, 1f - f.leak.y, f.leak.radius))
            program.setFloatsUniform("uLeakColor", floatArrayOf(f.leak.r, f.leak.g, f.leak.b))
            program.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (e: Exception) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            program.delete()
        } catch (e: Exception) {
            throw VideoFrameProcessingException(e)
        }
    }

    private companion object {
        const val VERTEX = """
attribute vec4 aFramePosition;
varying vec2 vUv;
void main() {
  gl_Position = aFramePosition;
  vUv = aFramePosition.xy * 0.5 + 0.5;
}
"""
        const val FRAGMENT = """
precision highp float;
uniform sampler2D uTexSampler;
uniform vec2 uRes;
uniform float uGrain, uGrainPx, uGrainChroma, uGrainSeed;
uniform float uHalation, uBloom, uVignette, uFlash, uSoft;
uniform float uScan, uScanPeriod, uBleed, uFade, uExposure;
uniform vec2 uWeave;
uniform vec4 uLeak;
uniform vec3 uLeakColor;
varying vec2 vUv;

vec3 T(vec2 uv) { return texture2D(uTexSampler, uv).rgb; }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
float rand(vec2 cell, float salt) {
  return fract(sin((cell.x + salt) * 12.9898 + (cell.y + uGrainSeed * 0.37) * 78.233) * 43758.5453);
}

void main() {
  vec2 texel = 1.0 / uRes;
  float aspect = uRes.x / uRes.y;
  vec2 uv = vUv + uWeave * texel;
  vec2 p = uv - 0.5;
  float rn = length(vec2(p.x * aspect, p.y)) / (0.5 * sqrt(aspect * aspect + 1.0));
  float rn2 = rn * rn;
  float barrel = 1.0 - 0.06 * uSoft * rn2;
  float ca = 0.010 * uSoft * rn2;
  vec2 gp = 0.5 + p * barrel;
  vec3 c = vec3(T(0.5 + p * barrel * (1.0 + ca)).r, T(gp).g, T(0.5 + p * barrel * (1.0 - ca)).b);
  if (uSoft > 0.0) {
    float eb = uSoft * smoothstep(0.35, 1.0, rn);
    if (eb > 0.0) {
      vec2 r = 3.0 * eb * texel;
      vec3 b = 0.25 * (T(gp + vec2(-r.x, -r.y)) + T(gp + vec2(r.x, -r.y)) + T(gp + vec2(-r.x, r.y)) + T(gp + vec2(r.x, r.y)));
      c = mix(c, b, min(1.0, eb * 0.9));
    }
  }
  if (uBleed > 0.0) {
    float bs = max(1.0, uRes.y / 360.0) * texel.x;
    vec3 s = 0.25 * (T(uv + vec2(-4.0 * bs, 0.0)) + T(uv + vec2(-9.0 * bs, 0.0)) + T(uv + vec2(4.0 * bs, 0.0)) + T(uv + vec2(9.0 * bs, 0.0)));
    float l = luma(c);
    float m = uBleed * 0.8;
    c = vec3(l) + (c - vec3(l)) * (1.0 - m) + (s - vec3(luma(s))) * m;
  }
  if (uHalation > 0.0) {
    vec2 rr = vec2(0.012 * uRes.y / uRes.x, 0.012);
    vec3 h = vec3(0.0);
    for (int i = 0; i < 12; i++) {
      float fi = float(i);
      float a = fi * 2.39996323;
      vec3 s = T(uv + vec2(cos(a), sin(a)) * sqrt((fi + 0.5) / 12.0) * rr);
      h += s * smoothstep(0.65, 1.0, luma(s));
    }
    float hl = luma(h) / 12.0;
    c += uHalation * 1.6 * hl * vec3(1.0, 0.38, 0.14);
  }
  if (uBloom > 0.0) {
    vec2 rr = vec2(0.03 * uRes.y / uRes.x, 0.03);
    vec3 h = vec3(0.0);
    vec3 avg = vec3(0.0);
    for (int i = 0; i < 12; i++) {
      float fi = float(i);
      float a = fi * 2.39996323;
      vec3 s = T(uv + vec2(cos(a), sin(a)) * sqrt((fi + 0.5) / 12.0) * rr);
      h += s * smoothstep(0.55, 1.0, luma(s));
      avg += s;
    }
    c = c + (avg / 12.0 - c) * (uBloom * 0.25) + uBloom * 0.9 * h / 12.0;
  }
  if (uFlash > 0.0) {
    float fall = 1.0 - smoothstep(0.0, 0.95, rn);
    float gain = 1.0 + uFlash * (0.5 * fall - 0.28 * (1.0 - fall));
    float warm = uFlash * fall;
    c *= gain * vec3(1.0 + 0.10 * warm, 1.0, 1.0 - 0.14 * warm);
  }
  c *= uExposure;
  if (uLeak.x > 0.0) {
    float lk = uLeak.x * smoothstep(uLeak.w, 0.0, length(vec2((uv.x - uLeak.y) * aspect, uv.y - uLeak.z)));
    c = 1.0 - (1.0 - c) * (1.0 - uLeakColor * lk);
  }
  if (uScan > 0.0) {
    c *= 1.0 - uScan * 0.22 * (0.5 + 0.5 * cos(6.28318531 * floor(gl_FragCoord.y) / uScanPeriod));
  }
  if (uFade > 0.0) c = c * (1.0 - 0.25 * uFade) + 0.12 * uFade;
  if (uVignette > 0.0) c *= 1.0 - uVignette * smoothstep(0.35, 1.0, rn);
  if (uGrain > 0.0) {
    vec2 cell = floor(gl_FragCoord.xy / uGrainPx);
    float w = 1.0 - 0.75 * smoothstep(0.45, 1.0, luma(c));
    float n0 = rand(cell, 0.0) - 0.5;
    vec3 g = vec3(n0) * (1.0 - uGrainChroma)
      + vec3(rand(cell, 1.7) - 0.5, rand(cell, 3.1) - 0.5, rand(cell, 5.3) - 0.5) * uGrainChroma;
    c += g * uGrain * 0.35 * w;
  }
  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""
    }
}
