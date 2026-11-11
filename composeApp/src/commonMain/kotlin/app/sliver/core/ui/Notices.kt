package app.sliver.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** App-wide one-message channel for failures raised outside a screen's own state; shown by [NoticeHost]. */
object Notices {
    private val state = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = state

    fun show(text: String) {
        state.value = text
    }

    fun clear() {
        state.value = null
    }
}

@Composable
fun NoticeHost() {
    val message by Notices.message.collectAsState()
    NoticeSheet(message, Notices::clear)
}
