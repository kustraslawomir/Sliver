package app.sliver.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.sliver.core.theme.SliverColors
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun SystemTimePickerDialog(initial: LocalTime, is24Hour: Boolean, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour)
    val scheme = if (SliverColors.light) lightColorScheme(
        primary = SliverColors.AccentText, surface = SliverColors.Surface1, onSurface = SliverColors.TextPrimary,
    ) else darkColorScheme(
        primary = SliverColors.Accent, surface = SliverColors.Surface2, onSurface = SliverColors.TextPrimary,
    )
    MaterialTheme(colorScheme = scheme) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surface) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Pick a time", color = scheme.onSurface)
                    TimePicker(state, Modifier, TimePickerDefaults.colors())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onDismiss) { Text("Cancel") }
                        TextButton({ onConfirm(LocalTime(state.hour, state.minute)) }) { Text("OK") }
                    }
                }
            }
        }
    }
}
