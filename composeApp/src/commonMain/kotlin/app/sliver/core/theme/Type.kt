package app.sliver.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * v1.7 type scale (design/png/GlassKit.png §1). Geist only, nothing under 13 sp, everything in sp so it follows the
 * system font scale. The only place in the app allowed to name a font size (scripts/design-lint.sh).
 * DECISION: lives in core/theme next to the other theme files (the task names ui/theme, which this project does not have).
 */
object ReelogType {
    private val Base get() = TextStyle(
        platformStyle = noFontPadding,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        fontFamily = SliverFonts.Sans,
        color = SliverColors.TextPrimary,
    )

    val display get() = Base.copy(fontSize = Size.display, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
    val title get() = Base.copy(fontSize = Size.title, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
    val headline get() = Base.copy(fontSize = Size.headline, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val body get() = Base.copy(fontSize = Size.body, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
    val label get() = Base.copy(fontSize = Size.label, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    /** HubV18 film poster title, 20/600. */
    val poster get() = Base.copy(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
    /** Calendar day number: the board asks 12/500; 13 sp is the app's minimum text size. */
    val cellNumber get() = Base.copy(fontSize = Size.caption, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val caption get() = Base.copy(fontSize = Size.caption, lineHeight = 18.sp, fontWeight = FontWeight.Normal, color = SliverColors.TextTertiary)

    /** Big clock on the reminder screen: the one numeric hero. */
    val clock get() = display.copy(fontSize = 64.sp, lineHeight = 68.sp, fontFeatureSettings = "tnum")

    /** Burned-in / on-video date stamp: the only mono text. */
    val stamp get() = Base.copy(fontSize = Size.caption, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFamily = SliverFonts.Mono, letterSpacing = 1.44.sp, color = SliverColors.TextSecondary)

    /** Sizes of the scale, for `.copy(fontSize = …)` on an existing style. */
    object Size {
        val display = 40.sp
        val title = 28.sp
        val headline = 22.sp
        val body = 17.sp
        val label = 15.sp
        val caption = 13.sp
    }

    /** Line height that goes with each size of the scale. */
    fun lineFor(size: TextUnit): TextUnit = when {
        size.value >= 40f -> 44.sp
        size.value >= 28f -> 32.sp
        size.value >= 22f -> 28.sp
        size.value >= 17f -> 24.sp
        size.value >= 15f -> 20.sp
        else -> 18.sp
    }
}

/** Tabular figures for times, counts and the scrubber. */
fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")
