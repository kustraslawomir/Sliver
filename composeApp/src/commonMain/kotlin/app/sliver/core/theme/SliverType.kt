package app.sliver.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

object SliverFonts {
    var Sans by mutableStateOf<FontFamily?>(null)
    var Mono by mutableStateOf<FontFamily?>(null)
}

/** Old style names, now mapped onto the v1.7 scale in [ReelogType]. */
object SliverType {
    val DisplayXL get() = ReelogType.display
    val DisplayL get() = ReelogType.display
    val DisplayM get() = ReelogType.title
    val Title get() = ReelogType.headline
    val DisplayS get() = Title

    /** List rows and row titles: 17 sp. */
    val Headline get() = ReelogType.body.copy(fontWeight = FontWeight.Medium)
    val Body get() = ReelogType.body
    val BodySmall get() = ReelogType.label.copy(fontWeight = FontWeight.Normal, color = SliverColors.TextSecondary)
    val Caption get() = ReelogType.caption
    val Num get() = ReelogType.label.tabular()
    val Meta get() = ReelogType.caption.copy(fontWeight = FontWeight.Medium, color = SliverColors.TextSecondary).tabular()

    /** Caption-size Geist with tabular figures: grid and poster labels. */
    fun tnum(tracking: TextUnit = TextUnit.Unspecified, color: Color = SliverColors.TextPrimary) =
        ReelogType.caption.copy(color = color, letterSpacing = tracking).tabular()

    /** Burned-in date stamp: the only uppercase mono text. */
    val Stamp get() = ReelogType.stamp
}

/** Android pads text with font ascent/descent unless told not to; other platforms have no such padding. */
internal expect val noFontPadding: androidx.compose.ui.text.PlatformTextStyle?
