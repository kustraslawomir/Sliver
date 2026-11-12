package app.sliver.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sliver.core.theme.generated.TokenText
import app.sliver.core.theme.generated.Tokens

object SliverFonts {
    var Sans by mutableStateOf<FontFamily?>(null)
    var Mono by mutableStateOf<FontFamily?>(null)
}

object SliverType {
    private val Base = TextStyle(
        platformStyle = noFontPadding,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )

    private fun TextStyle.token(t: TokenText, color: Color) = copy(
        color, t.size.sp, FontWeight(t.weight), fontFamily = SliverFonts.Sans, lineHeight = t.line.sp,
        letterSpacing = if (t.tracking == 0f) TextUnit.Unspecified else t.tracking.em,
    )

    // Sizes, line heights, weights and tracking come from tokens.json.
    val DisplayXL get() = Base.token(Tokens.DisplayXL, SliverColors.TextPrimary)
    val DisplayL get() = Base.token(Tokens.DisplayL, SliverColors.TextPrimary)
    val DisplayM get() = Base.token(Tokens.DisplayM, SliverColors.TextPrimary)
    val Title get() = Base.token(Tokens.Title, SliverColors.TextPrimary)
    val DisplayS get() = Title
    val Headline get() = Base.token(Tokens.Headline, SliverColors.TextPrimary)
    val Body get() = Base.token(Tokens.Body, SliverColors.TextPrimary)
    val BodySmall get() = Base.token(Tokens.BodySmall, SliverColors.TextSecondary)
    val Caption get() = Base.token(Tokens.Caption, SliverColors.TextTertiary)
    val Num get() = Base.copy(SliverColors.TextPrimary, 15.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 20.sp, fontFeatureSettings = "tnum")
    val Meta get() = Base.copy(SliverColors.TextSecondary, 12.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 16.sp, fontFeatureSettings = "tnum")
    /** Geist with tabular figures at a given size, regular weight: grid and poster labels. */
    fun tnum(size: TextUnit, tracking: TextUnit, color: Color = SliverColors.TextPrimary) =
        Meta.copy(color = color, fontSize = size, letterSpacing = tracking, fontWeight = FontWeight.Normal, lineHeight = TextUnit.Unspecified)

    /** Burned-in date stamp: the only uppercase mono text. */
    val Stamp get() = Base.copy(SliverColors.TextSecondary, 12.sp, FontWeight.Medium, fontFamily = SliverFonts.Mono, lineHeight = 16.sp, letterSpacing = 1.44.sp)
}

/** Android pads text with font ascent/descent unless told not to; other platforms have no such padding. */
internal expect val noFontPadding: androidx.compose.ui.text.PlatformTextStyle?
