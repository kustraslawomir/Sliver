package app.sliver

import app.sliver.core.ui.WindowClass
import app.sliver.core.ui.WindowSize
import app.sliver.core.ui.windowClassFor
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowSizeTest {
    @Test fun breakpoints() {
        assertEquals(WindowClass.Compact, windowClassFor(599f))
        assertEquals(WindowClass.Medium, windowClassFor(600f))
        assertEquals(WindowClass.Medium, windowClassFor(839f))
        assertEquals(WindowClass.Expanded, windowClassFor(840f))
    }

    @Test fun sideBySideNeedsRoomAndLandscape() {
        assertTrue(WindowSize(900.dp, 600.dp).sideBySide)
        assertFalse(WindowSize(700.dp, 1000.dp).sideBySide)
        assertFalse(WindowSize(500.dp, 400.dp).sideBySide)
    }
}
