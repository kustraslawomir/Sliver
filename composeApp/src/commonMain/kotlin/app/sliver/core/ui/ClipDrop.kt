package app.sliver.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate

/** The day whose clip was just saved: its Project cell pops in once (scale 0.6 to 1, 420 ms) with a ring. */
object ClipDrop {
    var pending by mutableStateOf<LocalDate?>(null)

    fun clear() {
        pending = null
    }
}
