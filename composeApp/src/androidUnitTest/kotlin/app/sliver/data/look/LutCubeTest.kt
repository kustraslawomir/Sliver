package app.sliver.data.look

import kotlin.test.Test
import kotlin.test.assertEquals

class LutCubeTest {
    private fun cube(): String = buildString {
        appendLine("TITLE \"t\"")
        appendLine("LUT_3D_SIZE 2")
        for (b in 0..1) for (g in 0..1) for (r in 0..1) appendLine("${1 - r} ${1 - g} ${1 - b}")
    }

    @Test
    fun parsesRedFastestAndInverts() {
        val c = LutCube.parse(cube().byteInputStream())
        assertEquals(0xFFFFFFFF.toInt(), c[0][0][0])
        assertEquals(0xFF00FFFF.toInt(), c[1][0][0])
        assertEquals(0xFFFFFF00.toInt(), c[0][0][1])
    }

    @Test
    fun zeroIntensityIsIdentity() {
        val c = LutCube.parse(cube().byteInputStream(), 0f)
        assertEquals(0xFF000000.toInt(), c[0][0][0])
        assertEquals(0xFFFF0000.toInt(), c[1][0][0])
    }

    @Test
    fun halfIntensityMixes() {
        val c = LutCube.parse(cube().byteInputStream(), 0.5f)
        assertEquals(0xFF808080.toInt(), c[0][0][0])
    }
}
