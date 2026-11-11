package app.sliver

import app.sliver.core.theme.SliverColors
import kotlin.test.Test
import kotlin.test.assertEquals

class SliverColorsTest {
    @Test
    fun backgroundIsNearBlack() {
        assertEquals(0xFF0A0A0B.toInt(), SliverColors.BgBase.value.shr(32).toInt())
    }
}
