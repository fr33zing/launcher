package dev.fr33zing.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Composable
fun CancelButton(onClick: () -> Unit) =
    FormButton(color = Catppuccin.Current.red, Icons.Filled.Close, onClick)

@Composable
fun FinishButton(onClick: () -> Unit) =
    FormButton(color = Catppuccin.Current.green, Icons.Filled.Check, onClick)

@Composable
fun FormButton(color: Color, icon: ImageVector, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color, circular = true)

    Box(
        Modifier.padding(horizontal = 8.dp).clickable(interactionSource, indication) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    ) {
        Icon(
            icon,
            contentDescription = "cancel",
            tint = color,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
