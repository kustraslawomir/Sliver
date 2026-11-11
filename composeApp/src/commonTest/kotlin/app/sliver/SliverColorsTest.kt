package app.sliver

import app.sliver.core.theme.SliverColors
import app.sliver.core.ui.UndoToastState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SliverColorsTest {
    @Test
    fun backgroundIsNearBlack() {
        assertEquals(0xFF0A0A0B.toInt(), SliverColors.BgBase.value.shr(32).toInt())
    }
}

class UndoToastStateTest {
    @Test
    fun actionRunsAndDismisses() {
        val s = UndoToastState()
        var undone = false
        s.show("x") { undone = true }
        s.performAction()
        assertTrue(undone)
        assertNull(s.current)
    }

    @Test
    fun newToastReplacesOld() {
        val s = UndoToastState()
        s.show("a")
        s.show("b")
        assertEquals("b", s.current?.text)
    }
}
