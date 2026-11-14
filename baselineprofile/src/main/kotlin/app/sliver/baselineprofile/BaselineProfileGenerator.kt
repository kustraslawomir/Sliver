package app.sliver.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PKG = "skustra.fragments"
private const val WAIT_MS = 5_000L

/** Cold start -> project screen (scroll) -> camera -> film builder (play) -> film player. Emulator/Xiaomi only. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PKG, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("Switch project")), WAIT_MS)
        device.findObject(By.scrollable(true))?.let { it.scroll(Direction.DOWN, 1f); it.scroll(Direction.UP, 1f) }

        // Camera: from the Record today dock, or today's day screen -> Replace.
        val dock = device.findObject(By.desc("Record today"))
        if (dock != null) dock.click() else {
            device.findObject(By.descContains("Today is in"))?.click()
            device.wait(Until.hasObject(By.text("Replace")), WAIT_MS)
            device.findObject(By.text("Replace"))?.click()
        }
        device.wait(Until.hasObject(By.desc("Record")), WAIT_MS)
        device.waitForIdle()
        device.pressBack()
        device.wait(Until.hasObject(By.desc("Switch project")), WAIT_MS)
        device.pressBack()
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("Switch project")), WAIT_MS)

        // Film builder: open, play a moment, back.
        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1f)
        device.wait(Until.hasObject(By.text("Make film")), WAIT_MS)
        device.findObject(By.text("Make film"))?.click()
        device.wait(Until.hasObject(By.text("Look")), WAIT_MS)
        device.findObject(By.desc("Clean filter"))?.click()
        device.waitForIdle()
        device.pressBack()
        device.wait(Until.hasObject(By.desc("Switch project")), WAIT_MS)

        // Film player: the first poster (its description ends with the film's length).
        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1f)
        device.findObject(By.descContains(":"))?.click()
        device.waitForIdle()
        device.pressBack()
    }
}
