package app.sliver

import app.sliver.data.LookPreset
import app.sliver.data.export.SevenSegment
import app.sliver.data.export.stampText
import app.sliver.data.StampStyle
import app.sliver.data.info
import app.sliver.data.look.LookModel
import app.sliver.data.look.LookRandom
import app.sliver.data.look.LookRaster
import app.sliver.data.look.LutGenerator
import app.sliver.data.look.lookFrame
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LookModelTest {
    private fun near(a: Double, b: Double, eps: Double = 1e-9) = assertTrue(abs(a - b) < eps, "$a vs $b")

    @Test
    fun identityModelLeavesColoursAlone() {
        for (c in listOf(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(1.0, 1.0, 1.0), doubleArrayOf(0.8, 0.4, 0.2), doubleArrayOf(0.1, 0.5, 0.9))) {
            val o = LutGenerator.apply(c, LookModel())
            for (i in 0..2) near(c[i], o[i])
        }
    }

    @Test
    fun blackLiftRaisesBlackToTheLiftAndKeepsWhite() {
        val m = LookModel(blackLift = 0.07)
        val black = LutGenerator.apply(doubleArrayOf(0.0, 0.0, 0.0), m)
        near(0.07, black[0]); near(0.07, black[1]); near(0.07, black[2])
        near(1.0, LutGenerator.apply(doubleArrayOf(1.0, 1.0, 1.0), m)[0])
    }

    @Test
    fun warmWhiteBalanceShiftsRedUpAndBlueDown() {
        val o = LutGenerator.apply(doubleArrayOf(0.5, 0.5, 0.5), LookModel(temp = 0.5))
        assertTrue(o[0] > 0.5 && o[2] < 0.5, o.toList().toString())
    }

    @Test
    fun monoMixUsesTheChannelWeights() {
        val m = LookModel(mono = listOf(0.5, 0.4, 0.1))
        val o = LutGenerator.apply(doubleArrayOf(1.0, 0.0, 0.0), m)
        for (i in 0..2) near(0.5, o[i])
        val g = LutGenerator.apply(doubleArrayOf(0.0, 1.0, 0.0), m)
        near(0.4, g[1])
    }

    @Test
    fun contrastCurveIsAnchoredAtBlackAndWhiteAndDarkensShadows() {
        val m = LookModel(contrast = 0.6)
        near(0.0, LutGenerator.apply(doubleArrayOf(0.0, 0.0, 0.0), m)[0])
        near(1.0, LutGenerator.apply(doubleArrayOf(1.0, 1.0, 1.0), m)[0])
        assertTrue(LutGenerator.apply(doubleArrayOf(0.25, 0.25, 0.25), m)[0] < 0.25)
        assertTrue(LutGenerator.apply(doubleArrayOf(0.75, 0.75, 0.75), m)[0] > 0.75)
    }

    @Test
    fun hueBandsOnlyTouchTheirOwnHue() {
        val m = LookModel(bands = listOf(0.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        val red = LutGenerator.apply(doubleArrayOf(0.9, 0.1, 0.1), m)
        assertTrue(red[0] - red[1] < 0.05, "red should be desaturated: ${red.toList()}")
        val blue = LutGenerator.apply(doubleArrayOf(0.1, 0.1, 0.9), m)
        near(0.9, blue[2], 1e-6)
    }

    @Test
    fun tableAndCubeTextRoundTrip() {
        val m = LookPreset.KIOSK.info.model
        val t = LutGenerator.table(m)
        assertEquals(33 * 33 * 33 * 3, t.size)
        assertTrue(t.all { it in 0f..1f })
        val parsed = LutGenerator.parseCube(LutGenerator.cube("t", m))
        assertEquals(t.size, parsed.size)
        for (i in t.indices step 997) assertTrue(abs(t[i] - parsed[i]) < 1e-5f)
    }

    @Test
    fun lutSampleHitsLatticePointsExactly() {
        val m = LookModel(temp = 0.4, contrast = 0.3)
        val t = LutGenerator.table(m)
        val out = FloatArray(3)
        LutGenerator.sample(t, 16f / 32f, 8f / 32f, 24f / 32f, out)
        val expect = LutGenerator.apply(doubleArrayOf(0.5, 0.25, 0.75), m)
        for (i in 0..2) assertTrue(abs(out[i] - expect[i]) < 1e-5, "$i ${out[i]} ${expect[i]}")
    }

    @Test
    fun clipSeedsAreStableAndDistinct() {
        assertEquals(LookRandom.seedOf("clip-a"), LookRandom.seedOf("clip-a"))
        assertNotEquals(LookRandom.seedOf("clip-a"), LookRandom.seedOf("clip-b"))
    }

    @Test
    fun jitterIsDeterministicPerFrameAndWithinBounds() {
        val seed = LookRandom.seedOf("clip-a")
        assertEquals(LookRandom.jitter(seed, 5, 1f), LookRandom.jitter(seed, 5, 1f))
        assertNotEquals(LookRandom.jitter(seed, 5, 1f), LookRandom.jitter(seed, 6, 1f))
        for (f in 0L until 500L) {
            val j = LookRandom.jitter(seed, f, 1f)
            assertTrue(abs(j.dx) <= 0.5f && abs(j.dy) <= 0.5f, "weave within half a pixel")
            assertTrue(abs(j.exposure - 1f) <= 0.015f + 1e-6f, "flicker within 1.5 %")
        }
        val none = LookRandom.jitter(seed, 3, 0f)
        assertEquals(0f, none.dx); assertEquals(0f, none.dy)
    }

    @Test
    fun leakProbabilityDecidesPerClipDeterministically() {
        val seeds = (0 until 400).map { LookRandom.seedOf("clip-$it") }
        assertTrue(seeds.none { LookRandom.hasLeak(it, 0.0) })
        assertTrue(seeds.all { LookRandom.hasLeak(it, 1.0) })
        val hits = seeds.count { LookRandom.hasLeak(it, 0.2) }
        assertTrue(hits in 40..120, "about 20 % of clips leak, got $hits")
        val s = seeds.first { LookRandom.hasLeak(it, 0.2) }
        assertEquals(LookRandom.leak(s, 0.2, 1.5f), LookRandom.leak(s, 0.2, 1.5f))
        assertTrue(LookRandom.leak(s, 0.2, 1.5f).strength > 0f)
        val clean = seeds.first { !LookRandom.hasLeak(it, 0.2) }
        assertEquals(0f, LookRandom.leak(clean, 0.2, 1.5f).strength)
    }

    @Test
    fun leakDriftsSlowly() {
        val s = LookRandom.seedOf("leaky")
        val a = LookRandom.leak(s, 1.0, 0f)
        val b = LookRandom.leak(s, 1.0, 0.04f)
        assertTrue(abs(a.x - b.x) + abs(a.y - b.y) < 0.02f)
    }

    @Test
    fun frameUniformsAreAPureFunctionOfClipAndTime() {
        val m = LookPreset.KIOSK.info.model
        val seed = LookRandom.seedOf("2026-11-10.mp4")
        assertEquals(lookFrame(m, 1f, 0.65f, seed, 1_000_000, 1080), lookFrame(m, 1f, 0.65f, seed, 1_000_000, 1080))
        assertNotEquals(lookFrame(m, 1f, 0.65f, seed, 1_000_000, 1080).grainSeed, lookFrame(m, 1f, 0.65f, seed, 1_100_000, 1080).grainSeed)
        assertEquals(
            lookFrame(m, 1f, 0.65f, seed, 0, 1080, animate = false),
            lookFrame(m, 1f, 0.65f, seed, 900_000, 1080, animate = false),
        )
    }

    @Test
    fun grainCellScalesWithOutputHeightAndIntensityScalesTheRest() {
        val m = LookPreset.KIOSK.info.model
        val hd = lookFrame(m, 1f, 0.5f, 1, 0, 1080)
        val uhd = lookFrame(m, 1f, 0.5f, 1, 0, 2160)
        near(2.0, hd.grainPx.toDouble(), 1e-5)
        near(4.0, uhd.grainPx.toDouble(), 1e-5)
        val half = lookFrame(m, 0.5f, 0.5f, 1, 0, 1080)
        near(hd.vignette / 2.0, half.vignette.toDouble(), 1e-6)
        assertEquals(hd.grain, half.grain, "grain has its own slider")
    }

    @Test
    fun grainIsStrongerInShadowsThanHighlights() {
        val w = 64; val h = 64
        val m = LookModel(grain = 1.0, grainSize = 1.0)
        fun spread(level: Int): Double {
            val src = IntArray(w * h) { (0xFF shl 24) or (level shl 16) or (level shl 8) or level }
            val out = LookRaster.render(src, w, h, null, 0f, lookFrame(m, 1f, 1f, 3, 0, 270))
            val v = out.map { (it and 255).toDouble() }
            val mean = v.average()
            return v.sumOf { (it - mean) * (it - mean) } / v.size
        }
        assertTrue(spread(80) > spread(245) * 2, "grain weaker in highlights")
    }

    @Test
    fun colourGrainDiffersPerChannelAndMonoGrainDoesNot() {
        val w = 32; val h = 32
        val src = IntArray(w * h) { 0xFF606060.toInt() }
        fun channelsEqual(chroma: Double): Boolean {
            val m = LookModel(grain = 1.0, grainSize = 1.0, grainChroma = chroma)
            val out = LookRaster.render(src, w, h, null, 0f, lookFrame(m, 1f, 1f, 5, 0, 270))
            return out.all { ((it shr 16) and 255) == (it and 255) }
        }
        assertTrue(channelsEqual(0.0))
        assertFalse(channelsEqual(1.0))
    }

    @Test
    fun rasterIsIdenticalForIdenticalInputs() {
        val w = 48; val h = 27
        val src = IntArray(w * h) { 0xFF000000.toInt() or (it * 2654435761L.toInt() and 0xFFFFFF) }
        val f = lookFrame(LookPreset.HALO.info.model, 1f, 0.4f, 11, 2_000_000, 270)
        assertContentEquals(LookRaster.render(src, w, h, null, 0f, f), LookRaster.render(src, w, h, null, 0f, f))
    }

    @Test
    fun halationAddsRedOrangeGlowAroundABrightPoint() {
        val w = 400; val h = 400
        val src = IntArray(w * h) { 0xFF000000.toInt() }
        for (y in 190..209) for (x in 190..209) src[y * w + x] = 0xFFFFFFFF.toInt()
        val f = lookFrame(LookModel(halation = 1.0), 1f, 0f, 1, 0, 1080)
        val out = LookRaster.render(src, w, h, null, 0f, f)
        val beside = out[200 * w + 212]
        val r = (beside shr 16) and 255; val b = beside and 255
        assertTrue(r > 0 && r > b, "glow beside the light is red-orange: r=$r b=$b")
        assertEquals(0xFF000000.toInt(), out[10 * w + 10])
    }

    @Test
    fun flashFalloffBrightensTheCentreAndDarkensTheEdges() {
        val w = 64; val h = 36
        val src = IntArray(w * h) { 0xFF808080.toInt() }
        val out = LookRaster.render(src, w, h, null, 0f, lookFrame(LookModel(flash = 1.0), 1f, 0f, 1, 0, 540))
        val c = out[(h / 2) * w + w / 2] and 255
        val e = out[0] and 255
        assertTrue(c > 0x80 && e < 0x80, "centre $c edge $e")
    }

    @Test
    fun identityFrameSkipsTheShader() {
        assertTrue(lookFrame(LookModel(), 1f, 0f, 1, 0, 1080).isIdentity)
        assertFalse(lookFrame(LookModel(grain = 0.1), 1f, 0.1f, 1, 0, 1080).isIdentity)
    }

    @Test
    fun ledStampIsDigitsAndAnApostrophe() {
        assertEquals("02 11 '26", stampText(LocalDate(2026, 11, 2), StampStyle.LED))
        assertEquals("2 NOV. 2026", stampText(LocalDate(2026, 11, 2), StampStyle.CAMCORDER))
        assertEquals("2 NOV 2026", stampText(LocalDate(2026, 11, 2), StampStyle.PLAIN))
    }

    @Test
    fun sevenSegmentLayout() {
        assertEquals(6, SevenSegment.rects("0", 16f).size)
        assertEquals(7, SevenSegment.rects("8", 16f).size)
        assertEquals(2, SevenSegment.rects("1", 16f).size)
        assertTrue(SevenSegment.width("88", 16f) > SevenSegment.width("8", 16f) * 1.9f)
        assertTrue(SevenSegment.rects("'26 11 02", 16f).all { it.x >= 0f && it.y >= 0f && it.y + it.h <= 16f + 1e-4f })
    }
}
