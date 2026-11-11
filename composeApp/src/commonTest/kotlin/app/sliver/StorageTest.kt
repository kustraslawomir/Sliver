package app.sliver

import app.sliver.core.ui.MIN_FREE_BYTES
import app.sliver.core.ui.isStorageLow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageTest {
    @Test fun belowThresholdIsLow() = assertTrue(isStorageLow(MIN_FREE_BYTES - 1))
    @Test fun atThresholdIsFine() = assertFalse(isStorageLow(MIN_FREE_BYTES))
}
