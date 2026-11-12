package app.sliver

import androidx.compose.ui.graphics.Color
import app.sliver.core.theme.SliverColors
import kotlin.math.pow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {
    private fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    private fun lum(c: Color) = 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
    private fun ratio(a: Color, b: Color) = (maxOf(lum(a), lum(b)) + 0.05f) / (minOf(lum(a), lum(b)) + 0.05f)

    @AfterTest fun reset() { SliverColors.userLight = false }

    private fun check(light: Boolean) {
        SliverColors.userLight = light
        val name = if (light) "light" else "dark"
        val pages = listOf(SliverColors.BgBase, SliverColors.Surface1, SliverColors.Surface2, SliverColors.Surface3)
        val text = listOf(
            "primary" to SliverColors.TextPrimary, "secondary" to SliverColors.TextSecondary, "tertiary" to SliverColors.TextTertiary,
            "accentText" to SliverColors.AccentText, "success" to SliverColors.Success, "warning" to SliverColors.Warning, "error" to SliverColors.Error,
        )
        for ((n, c) in text) for (bg in pages) assertTrue(ratio(c, bg) >= 4.5f, "$name $n on $bg = ${ratio(c, bg)}")
        for (bg in pages.take(2)) assertTrue(ratio(SliverColors.AccentMark, bg) >= 3f, "$name accentMark on $bg = ${ratio(SliverColors.AccentMark, bg)}")
        assertTrue(ratio(SliverColors.OnAccent, SliverColors.Accent) >= 4.5f, "$name onAccent")
        assertTrue(ratio(SliverColors.OnInverse, SliverColors.TextPrimary) >= 4.5f, "$name onInverse")
        assertTrue(ratio(SliverColors.TextTertiary, SliverColors.Surface3) >= 4.5f, "$name disabled primary label")
        assertTrue(ratio(SliverColors.OnMedia, SliverColors.Media) >= 4.5f, "$name onMedia")
        assertTrue(ratio(SliverColors.Accent, SliverColors.Media) >= 4.5f, "$name accent on media")
    }

    @Test fun darkMeetsAa() = check(false)
    @Test fun lightMeetsAa() = check(true)
}
