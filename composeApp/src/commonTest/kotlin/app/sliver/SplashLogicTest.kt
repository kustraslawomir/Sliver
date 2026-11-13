package app.sliver

import app.sliver.core.ui.SPLASH_DISC_DP
import app.sliver.core.ui.SPLASH_WORD_DISC_DX
import app.sliver.core.ui.splashFrame
import kotlin.test.Test
import kotlin.test.assertEquals

class SplashLogicTest {
    private fun at(t: Float) = splashFrame(t, centerY = 400f, dockY = 760f)

    @Test fun startsAsTheSystemSplashDisc() {
        val f = at(260f)
        assertEquals(SPLASH_DISC_DP, f.discSize)
        assertEquals(0f, f.discDx)
        assertEquals(400f, f.discY)
        assertEquals(0f, f.letters)
        assertEquals(1f, f.background)
    }

    @Test fun holdsTheWordWithTagline() {
        val f = at(1000f)
        assertEquals(28f, f.discSize, 0.01f)
        assertEquals(SPLASH_WORD_DISC_DX, f.discDx, 0.01f)
        assertEquals(0f, f.reelShift)
        assertEquals(0f, f.gShift)
        assertEquals(1f, f.letters)
        assertEquals(1f, f.tagline, 0.01f)
    }

    @Test fun endsOnTheRecordButton() {
        val f = at(1500f)
        assertEquals(app.sliver.core.ui.DOCK_DISC_DP, f.discSize, 0.01f)
        assertEquals(0f, f.discDx, 0.01f)
        assertEquals(760f, f.discY, 0.01f)
        assertEquals(0f, f.letters)
        assertEquals(0f, f.background, 0.01f)
    }
}
