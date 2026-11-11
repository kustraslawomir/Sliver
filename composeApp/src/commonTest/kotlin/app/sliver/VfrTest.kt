package app.sliver

import app.sliver.data.export.isVariableFrameRate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VfrTest {
    @Test fun steadyRateIsNotVariable() {
        assertFalse(isVariableFrameRate(List(60) { it * 33_333L }))
        assertFalse(isVariableFrameRate(List(60) { it * 33_333L + if (it % 2 == 0) 1_000 else 0 }))
    }

    @Test fun mixedGapsAreVariable() {
        var t = 0L
        val times = List(60) { i -> t.also { t += if (i % 3 == 0) 66_000 else 33_000 } }
        assertTrue(isVariableFrameRate(times))
    }
}
