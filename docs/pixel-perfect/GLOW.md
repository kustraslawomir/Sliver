# Glow is MANDATORY

The ember glow is part of the brand, not decoration. A screen that has a glow in the design and none in the app **fails review**, the same as a wrong colour. "Looks close enough without it" doesn't count.

## 1. One modifier, used everywhere

`core/designsystem/Glow.kt`. Copy it as is:

```kotlin
data class GlowLayer(val color: Color, val blur: Dp, val dy: Dp = 0.dp)

/**
 * Draws soft light behind the element, like CSS box-shadow with no spread.
 * CSS "0 0 14px #FF8A3D88" → GlowLayer(Ember.copy(alpha = 0x88 / 255f), 14.dp)
 * Pass the SAME shape the element is clipped to. For outline-only elements (trim frame)
 * pass stroke = the border width.
 */
fun Modifier.glow(shape: Shape, vararg layers: GlowLayer, stroke: Dp? = null): Modifier =
    this.drawBehind {
        if (Build.VERSION.SDK_INT < 28) return@drawBehind   // HW shadow layers need API 28
        val outline = shape.createOutline(size, layoutDirection, this)
        drawIntoCanvas { canvas ->
            layers.forEach { l ->
                val paint = Paint().apply {
                    color = l.color
                    if (stroke != null) { style = PaintingStyle.Stroke; strokeWidth = stroke.toPx() }
                }
                paint.asFrameworkPaint().setShadowLayer(
                    l.blur.toPx() * 0.9f,          // CSS blur → Android shadow radius
                    0f, l.dy.toPx(), l.color.toArgb()
                )
                canvas.drawOutline(outline, paint)
            }
        }
    }

// Text glow (date stamp): TextStyle(shadow = Shadow(Ember.copy(alpha = .6f), blurRadius = 6.dp.toPx()))
```

Rules:
- Never use `Modifier.shadow()` or Material elevation for glow. They are grey and directional.
- Never use `Modifier.blur()` for glow, because it blurs the content too.
- The glow must not be clipped. Don't put `clip()` or `graphicsLayer { clip = true }` on a PARENT of a glowing element, and leave room around it: padding ≥ blur.
- Breathing glow (record button): animate the layers' alpha with `rememberInfiniteTransition`. If system animations are off (`ANIMATOR_DURATION_SCALE == 0`), keep it static.

## 2. The tokens (also in design/tokens.json → "glow")

| Token | Layers (CSS from the design) | Used on |
|---|---|---|
| `glow.cta` | `0 8px 32px #FF8A3D44` | every filled ember primary button (Save clip, Choose from Photos, Make film, Export…) |
| `glow.record` | `0 0 14px #FF8A3D88, 0 0 32px #FF8A3D33`; recording breathes to `0 0 20px #FF8A3DAA, 0 0 44px #FF8A3D44` (1.6 s) | record button (video) |
| `glow.frame` | `0 0 14px #FF8A3D99, 0 0 36px #FF8A3D55`, stroke 2.5dp | trim / keep-window frame on the strip |
| `glow.slit` | `0 0 12px #FF8A3D, 0 0 32px #FF8A3D88` | slit loader sweep, export slit, onboarding slit (hero: add `0 0 110px #FF8A3D88`) |
| `glow.dot` | `0 0 8px #FF8A3D` | small ember dots: progress heads, film builder/player markers, backup dot |
| `glow.lock` | `0 0 10px #FF8A3D99` | focus ring while AE/AF is locked |
| `glow.stamp` | text `0 0 6px #FF8A3D99` | date stamp on clips |

The photo-mode record button is white: `0 0 16px #FFFFFF33`.

## 3. Proof (part of "done")

For EVERY screen you touched that has a glow in the design, add a crop to `docs/qa/glow/` at 3× zoom, with the design crop next to it (`tools/compare.py --crop x,y,w,h`). The glow must be visible in the app crop. If it's missing or clipped, the task is not done.
