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

/** Sharpen (unsharp), fade, vignette and grain in a single pass. Amounts are 0..1. */
@UnstableApi
class LookShaderEffect(
    private val sharp: Float,
    private val grain: Float,
    private val vignette: Float,
    private val fade: Float,
    private val animateGrain: Boolean,
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        LookShaderProgram(useHdr, sharp, grain, vignette, fade, animateGrain)
}

@UnstableApi
private class LookShaderProgram(
    useHdr: Boolean,
    private val sharp: Float,
    private val grain: Float,
    private val vignette: Float,
    private val fade: Float,
    private val animateGrain: Boolean,
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
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatsUniform("uTexel", floatArrayOf(1f / width, 1f / height))
            program.setFloatUniform("uSharp", sharp)
            program.setFloatUniform("uGrain", grain)
            program.setFloatUniform("uVignette", vignette)
            program.setFloatUniform("uFade", fade)
            program.setFloatUniform("uTime", if (animateGrain) (presentationTimeUs / 1000 % 10_000) / 1000f else 0f)
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
uniform vec2 uTexel;
uniform float uSharp;
uniform float uGrain;
uniform float uVignette;
uniform float uFade;
uniform float uTime;
varying vec2 vUv;
float rand(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
  vec3 c = texture2D(uTexSampler, vUv).rgb;
  if (uSharp > 0.0) {
    vec3 blur = 0.25 * (texture2D(uTexSampler, vUv + vec2(uTexel.x, 0.0)).rgb
      + texture2D(uTexSampler, vUv - vec2(uTexel.x, 0.0)).rgb
      + texture2D(uTexSampler, vUv + vec2(0.0, uTexel.y)).rgb
      + texture2D(uTexSampler, vUv - vec2(0.0, uTexel.y)).rgb);
    c += (c - blur) * uSharp * 1.5;
  }
  c = c * (1.0 - 0.25 * uFade) + 0.12 * uFade;
  float d = distance(vUv, vec2(0.5)) * 1.4142;
  c *= 1.0 - uVignette * smoothstep(0.35, 1.0, d);
  c += (rand(vUv + uTime) - 0.5) * uGrain * 0.3;
  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""
    }
}
