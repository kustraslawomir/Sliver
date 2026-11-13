package app.sliver.shots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.sliver.feature.camera.RecordButton
import app.sliver.feature.camera.RecordState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** v1.7 record button states (idle · photo · recording 0.6 s) over a synthetic sunset, like design/png/RecordButton.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi", application = android.app.Application::class)
class RecordButtonShots : ShotTest() {
    @Test fun states() = shoot("RecordButtonV2", darkOnly = true, inspection = true) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF0A35E), Color(0xFF8A4A52), Color(0xFF1C1A22)))), // lint-ok: synthetic test backdrop
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                RecordButton(RecordState.VideoIdle, false, true, {}, {}, {})
                RecordButton(RecordState.Photo, false, true, {}, {}, {})
                RecordButton(RecordState.Recording, false, true, {}, {}, {}, elapsedMs = 600)
            }
        }
    }
}
