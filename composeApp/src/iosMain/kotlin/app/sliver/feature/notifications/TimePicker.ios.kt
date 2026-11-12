package app.sliver.feature.notifications

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalTime

@Composable
actual fun SystemTimePickerDialog(initial: LocalTime, is24Hour: Boolean, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit): Unit = TODO("iOS later")
