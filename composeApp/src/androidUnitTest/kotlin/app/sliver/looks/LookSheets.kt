package app.sliver.looks

import app.sliver.data.FilterPresets
import app.sliver.data.info
import app.sliver.data.look.LookModel
import app.sliver.data.look.LookRandom
import app.sliver.data.look.LookRaster
import app.sliver.data.look.LutGenerator
import app.sliver.data.look.lookFrame
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.sin

/**
 * Contact-sheet renderer (no Python, no native image libs): synthetic test frames, a CPU render of every look through
 * the real 33^3 LUT text and [LookRaster] (the CPU port of the GLSL pass), and Graphics2D labels.
 */
object LookSheets {
    const val TILE_W = 320
    const val TILE_H = 180

    class Frame(val name: String, val px: IntArray)

    private fun canvas(w: Int, h: Int, draw: (java.awt.Graphics2D) -> Unit): IntArray {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        draw(g)
        g.dispose()
        return img.getRGB(0, 0, w, h, null, 0, w).also { for (i in it.indices) it[i] = it[i] or (0xFF shl 24) }
    }

    private fun radial(g: java.awt.Graphics2D, cx: Float, cy: Float, r: Float, inner: Color, outer: Color) {
        g.paint = java.awt.RadialGradientPaint(cx, cy, r, floatArrayOf(0f, 1f), arrayOf(inner, outer))
        g.fill(Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r))
    }

    /** Warm skin-tone gradient portrait stand-in: face, hair, shoulders, window light. */
    fun portrait(w: Int = TILE_W, h: Int = TILE_H) = Frame("portrait", canvas(w, h) { g ->
        g.paint = java.awt.GradientPaint(0f, 0f, Color(120, 134, 140), w.toFloat(), 0f, Color(70, 62, 58))
        g.fillRect(0, 0, w, h)
        g.color = Color(214, 226, 236); g.fill(Rectangle2D.Float(w * 0.04f, h * 0.1f, w * 0.18f, h * 0.55f))
        g.color = Color(52, 48, 54)
        g.fill(Ellipse2D.Float(w * 0.18f, h * 0.78f, w * 0.64f, h * 0.7f))
        g.color = Color(40, 28, 24); g.fill(Ellipse2D.Float(w * 0.36f, h * 0.1f, w * 0.28f, h * 0.5f))
        radial(g, w * 0.5f, h * 0.42f, h * 0.27f, Color(238, 190, 156), Color(176, 118, 90))
        g.color = Color(120, 70, 60); g.fill(Ellipse2D.Float(w * 0.47f, h * 0.55f, w * 0.06f, h * 0.025f))
        g.color = Color(40, 26, 22); g.fill(Ellipse2D.Float(w * 0.43f, h * 0.38f, w * 0.03f, h * 0.025f)); g.fill(Ellipse2D.Float(w * 0.54f, h * 0.38f, w * 0.03f, h * 0.025f))
    })

    /** Green foliage pattern with dappled light. */
    fun foliage(w: Int = TILE_W, h: Int = TILE_H) = Frame("foliage", canvas(w, h) { g ->
        g.paint = java.awt.GradientPaint(0f, 0f, Color(30, 60, 28), 0f, h.toFloat(), Color(12, 28, 14))
        g.fillRect(0, 0, w, h)
        for (i in 0 until 140) {
            val x = (sin(i * 12.9898) * 43758.5453).let { (it - Math.floor(it)).toFloat() } * w
            val y = (sin(i * 78.233) * 24634.6345).let { (it - Math.floor(it)).toFloat() } * h
            val s = 14f + (i % 5) * 7f
            val shade = 40 + (i * 37) % 110
            g.color = Color(shade / 3, shade, shade / 4, 200)
            val leaf = Path2D.Float()
            leaf.moveTo(x, y); leaf.quadTo(x + s, y - s * 0.5f, x + s * 1.6f, y); leaf.quadTo(x + s, y + s * 0.5f, x, y)
            g.fill(leaf)
        }
        for (i in 0 until 24) { g.color = Color(250, 244, 190, 150); val x = (i * 97 % w).toFloat(); val y = (i * 53 % h).toFloat(); g.fill(Ellipse2D.Float(x, y, 9f, 7f)) }
    })

    /** Blue sky gradient, soft clouds, dark ground strip. */
    fun sky(w: Int = TILE_W, h: Int = TILE_H) = Frame("sky", canvas(w, h) { g ->
        g.paint = java.awt.GradientPaint(0f, 0f, Color(34, 96, 190), 0f, h * 0.72f, Color(176, 214, 244))
        g.fillRect(0, 0, w, h)
        for (i in 0 until 7) radial(g, w * (0.1f + 0.13f * i), h * (0.25f + 0.1f * ((i * 3) % 4)), h * 0.2f, Color(255, 255, 255, 230), Color(255, 255, 255, 0))
        g.color = Color(38, 44, 30); g.fillRect(0, (h * 0.78f).toInt(), w, h)
        g.color = Color(70, 80, 50); for (i in 0 until 14) g.fillRect(i * w / 14, (h * 0.74f).toInt(), w / 28, (h * 0.05f).toInt())
    })

    /** Night street: dark navy, bright points of several colours, lit windows. */
    fun night(w: Int = TILE_W, h: Int = TILE_H) = Frame("night", canvas(w, h) { g ->
        g.paint = java.awt.GradientPaint(0f, 0f, Color(8, 10, 24), 0f, h.toFloat(), Color(18, 16, 22))
        g.fillRect(0, 0, w, h)
        g.color = Color(26, 28, 40); g.fillRect((w * 0.05f).toInt(), (h * 0.2f).toInt(), (w * 0.3f).toInt(), (h * 0.8f).toInt()); g.fillRect((w * 0.62f).toInt(), (h * 0.1f).toInt(), (w * 0.3f).toInt(), (h * 0.9f).toInt())
        g.color = Color(255, 214, 140); for (r in 0 until 4) for (c in 0 until 3) g.fillRect((w * 0.08f + c * w * 0.08f).toInt(), (h * 0.27f + r * h * 0.14f).toInt(), (w * 0.04f).toInt(), (h * 0.07f).toInt())
        val lights = listOf(Triple(0.45f, 0.55f, Color(255, 250, 235)), Triple(0.52f, 0.62f, Color(255, 120, 60)), Triple(0.58f, 0.5f, Color(255, 40, 50)), Triple(0.4f, 0.68f, Color(90, 220, 255)), Triple(0.5f, 0.45f, Color(255, 255, 255)), Triple(0.75f, 0.35f, Color(255, 190, 90)))
        for ((x, y, c) in lights) { radial(g, w * x, h * y, h * 0.05f, c, Color(c.red, c.green, c.blue, 0)); g.color = c; g.fill(Ellipse2D.Float(w * x - 3, h * y - 3, 6f, 6f)) }
    })

    /** Seeder-style frames: the flat hue gradients the debug seeder and the screenshot harness use for demo clips. */
    fun demo(hue: Float, name: String, w: Int = TILE_W, h: Int = TILE_H) = Frame(name, canvas(w, h) { g ->
        val top = Color.getHSBColor(hue / 360f, 0.45f, 0.55f)
        val bottom = Color.getHSBColor(hue / 360f, 0.6f, 0.2f)
        g.paint = java.awt.GradientPaint(0f, 0f, top, 0f, h.toFloat(), bottom)
        g.fillRect(0, 0, w, h)
        g.color = Color.getHSBColor(((hue + 30) % 360) / 360f, 0.5f, 0.9f); g.fill(Ellipse2D.Float(w * 0.62f, h * 0.18f, h * 0.22f, h * 0.22f))
        g.color = Color.getHSBColor(hue / 360f, 0.5f, 0.12f); g.fillRect(0, (h * 0.75f).toInt(), w, h)
    })

    fun testFrames(): List<Frame> = listOf(portrait(), foliage(), sky(), night())
    fun demoFrames(): List<Frame> = listOf(demo(25f, "demo A"), demo(200f, "demo B"))

    private val tables = HashMap<LookModel, FloatArray?>()

    /** Renders [frame] through [model] using the real 33^3 LUT (cube text parsed back) and the CPU shader. */
    fun render(frame: Frame, model: LookModel, seedId: String, w: Int = TILE_W, h: Int = TILE_H): IntArray {
        val lut = tables.getOrPut(model.colourOnly()) { if (model.hasLut) LutGenerator.parseCube(LutGenerator.cube("sheet", model.colourOnly())) else null }
        return LookRaster.render(frame.px, w, h, lut, 1f, lookFrame(model, 1f, model.grain.toFloat(), LookRandom.seedOf(seedId), 0L, h, animate = false))
    }

    fun tile(g: java.awt.Graphics2D, px: IntArray, x: Int, y: Int, label: String?, w: Int = TILE_W, h: Int = TILE_H) {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        img.setRGB(0, 0, w, h, px, 0, w)
        g.drawImage(img, x, y, null)
        if (label != null) {
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            val fm = g.fontMetrics
            val tw = fm.stringWidth(label)
            g.color = Color(0, 0, 0, 170); g.fillRoundRect(x + 5, y + 5, tw + 10, 20, 6, 6)
            g.color = Color.WHITE; g.drawString(label, x + 10, y + 20)
        }
    }

    fun sheet(width: Int, height: Int, draw: (java.awt.Graphics2D) -> Unit): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(14, 14, 16); g.fillRect(0, 0, width, height)
        draw(g)
        g.dispose()
        return img
    }

    fun write(img: BufferedImage, file: File) {
        file.parentFile.mkdirs()
        ImageIO.write(img, "png", file)
    }

    fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    /** The A/B/C variants of a shipped preset: A as shipped; B warmer, lifted, grainier; C cooler, deeper, finer. */
    fun variants(base: LookModel): List<Pair<String, LookModel>> = listOf(
        "A" to base,
        "B" to base.copy(temp = base.temp + 0.25, blackLift = base.blackLift + 0.04, grain = (base.grain * 1.5).coerceAtMost(1.0)),
        "C" to base.copy(temp = base.temp - 0.25, blackLift = max(0.0, base.blackLift - 0.03), exposure = base.exposure - 0.15, grain = base.grain * 0.6),
    )

    fun tuningSheet(): BufferedImage {
        val frames = testFrames()
        val rows = FilterPresets.flatMap { p -> variants(p.info.model).map { (v, m) -> Triple(p.info.label, v, m) } }
        val labelW = 130
        val head = 44
        return sheet(labelW + frames.size * TILE_W, head + rows.size * TILE_H) { g ->
            g.color = Color.WHITE; g.font = Font(Font.SANS_SERIF, Font.BOLD, 15)
            g.drawString("Tuning sheet: A = as shipped, B = warmer + lifted blacks + more grain, C = cooler + deeper + finer grain", 8, 18)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 12); g.color = Color(180, 180, 180)
            g.drawString("Frames: portrait stand-in, foliage, sky, night lights. Grain is drawn in 1 px cells here, so it reads stronger than on a 1080p clip.", 8, 36)
            rows.forEachIndexed { i, (name, v, m) ->
                val y = head + i * TILE_H
                g.color = Color.WHITE; g.font = Font(Font.SANS_SERIF, Font.BOLD, 16); g.drawString("$name $v", 10, y + TILE_H / 2)
                frames.forEachIndexed { c, f -> tile(g, render(f, m, "tuning-$name-$c"), labelW + c * TILE_W, y, if (c == 0) "$name $v" else null) }
            }
        }
    }

    fun familySheet(family: String, items: List<app.sliver.data.look.LookCandidate>): BufferedImage {
        val frames = testFrames() + demoFrames().take(1)
        val head = 30
        return sheet(frames.size * TILE_W, head + items.size * TILE_H) { g ->
            g.color = Color.WHITE; g.font = Font(Font.SANS_SERIF, Font.BOLD, 16)
            g.drawString("$family: " + items.joinToString(", ") { "${it.number} ${it.name}" }, 8, 20)
            items.forEachIndexed { i, c ->
                frames.forEachIndexed { col, f -> tile(g, render(f, c.model, "family-${c.number}-$col"), col * TILE_W, head + i * TILE_H, "${c.number} ${c.name}") }
            }
        }
    }

    fun overview(all: List<app.sliver.data.look.LookCandidate>): BufferedImage {
        val cols = 5
        val rows = (all.size + cols - 1) / cols
        val frame = portrait()
        return sheet(cols * TILE_W, rows * TILE_H) { g ->
            all.forEachIndexed { i, c -> tile(g, render(frame, c.model, "overview-${c.number}"), (i % cols) * TILE_W, (i / cols) * TILE_H, "${c.number} ${c.name}") }
        }
    }
}
