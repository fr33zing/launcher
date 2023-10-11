package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.helper.conditional
import dev.fr33zing.launcher.ui.theme.outlinedTextFieldColors

@Composable
fun OutlinedValue(
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf<Dp?>(null) }

    OutlinedTextField(
        value = " ",
        onValueChange = {},
        colors = outlinedTextFieldColors(),
        label = { Text(label) },
        readOnly = true,
        enabled = !readOnly,
        modifier =
            modifier.then(
                Modifier.conditional(condition = contentHeight != null) {
                    animateContentSize().height(contentHeight!! + 42.dp)
                }
            ),
        prefix = {
            Box(
                Modifier.onGloballyPositioned {
                    contentHeight = with(density) { it.size.height.toDp() }
                }
            ) {
                content()
            }
        },
    )
}
