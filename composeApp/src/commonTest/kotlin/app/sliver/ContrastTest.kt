package app.sliver

import androidx.compose.ui.graphics.Color
import app.sliver.core.theme.SliverColors
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {
    private fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    private fun lum(c: Color) = 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
    private fun ratio(a: Color, b: Color) = (maxOf(lum(a), lum(b)) + 0.05f) / (minOf(lum(a), lum(b)) + 0.05f)

    @Test fun textMeetsAa() {
        val text = listOf("primary" to SliverColors.TextPrimary, "secondary" to SliverColors.TextSecondary, "tertiary" to SliverColors.TextTertiary,
            "accent" to SliverColors.Accent, "core" to SliverColors.AccentCore, "success" to SliverColors.Success,
            "warning" to SliverColors.Warning, "error" to SliverColors.Error)
        for ((n, c) in text) for (bg in listOf(SliverColors.BgBase, SliverColors.Surface1, SliverColors.Surface2)) {
            assertTrue(ratio(c, bg) >= 4.5f, "$n on $bg = ${ratio(c, bg)}")
        }
        assertTrue(ratio(SliverColors.OnAccent, SliverColors.Accent) >= 4.5f)
        assertTrue(ratio(SliverColors.OnAccent, SliverColors.TextPrimary) >= 4.5f)
    }
}
