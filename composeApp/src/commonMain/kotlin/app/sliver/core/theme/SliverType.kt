package app.sliver.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object SliverFonts {
    var Sans by mutableStateOf<FontFamily?>(null)
    var Mono by mutableStateOf<FontFamily?>(null)
}

object SliverType {
    private val Base = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )

    // Weight 600 = Geist SemiBold; tracking per tokens.json.
    val DisplayXL get() = Base.copy(SliverColors.TextPrimary, 52.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 56.sp, letterSpacing = (-0.03).em)
    val DisplayL get() = Base.copy(SliverColors.TextPrimary, 36.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 40.sp, letterSpacing = (-0.025).em)
    val DisplayM get() = Base.copy(SliverColors.TextPrimary, 24.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 30.sp, letterSpacing = (-0.02).em)
    val Title get() = Base.copy(SliverColors.TextPrimary, 22.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 28.sp)
    val DisplayS get() = Title
    val Headline get() = Base.copy(SliverColors.TextPrimary, 17.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 24.sp)
    val Body get() = Base.copy(SliverColors.TextPrimary, 17.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 26.sp)
    val BodySmall get() = Base.copy(SliverColors.TextSecondary, 15.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 22.sp)
    val Caption get() = Base.copy(SliverColors.TextTertiary, 13.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 18.sp)
    val Num get() = Base.copy(SliverColors.TextPrimary, 15.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 20.sp, fontFeatureSettings = "tnum")
    val Meta get() = Base.copy(SliverColors.TextSecondary, 12.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 16.sp, fontFeatureSettings = "tnum")
    /** Burned-in date stamp: the only uppercase mono text. */
    val Stamp get() = Base.copy(SliverColors.TextSecondary, 12.sp, FontWeight.Medium, fontFamily = SliverFonts.Mono, lineHeight = 16.sp, letterSpacing = 1.44.sp)
}
