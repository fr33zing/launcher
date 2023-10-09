package dev.fr33zing.launcher.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.fr33zing.launcher.ui.theme.outlinedTextFieldColors

@Composable
fun OutlinedReadOnlyValue(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    OutlinedTextField(
        value = " ",
        onValueChange = {},
        colors = outlinedTextFieldColors(),
        label = { Text(label) },
        readOnly = true,
        enabled = false,
        prefix = content,
        modifier = modifier
    )
}
