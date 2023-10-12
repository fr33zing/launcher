package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.helper.conditional
import dev.fr33zing.launcher.ui.theme.outlinedTextFieldColors

@Composable
fun OutlinedValue(
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf<Dp?>(null) }
    var contentWidth by remember { mutableStateOf<Dp?>(null) }
    val contentPadding = remember { PaddingValues(horizontal = 12.dp, vertical = 1.dp) }

    CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
        OutlinedTextField(
            value = " ",
            onValueChange = {},
            colors = outlinedTextFieldColors(),
            label = { Text(label) },
            readOnly = true,
            enabled = !readOnly,
            modifier =
                Modifier.conditional(condition = contentHeight != null) {
                        animateContentSize().height(contentHeight!! + 42.dp)
                    }
                    .focusProperties { canFocus = false }
                    .then(modifier),
            prefix = {
                Box(
                    Modifier.fillMaxWidth()
                        .onGloballyPositioned {
                            contentHeight = with(density) { it.size.height.toDp() }
                            contentWidth = with(density) { it.size.width.toDp() }
                        }
                        .offset(x = (-7).dp)
                        .conditional(contentWidth != null) { requiredWidth(contentWidth!! + 16.dp) }
                ) {
                    content(contentPadding)
                }
            },
        )
    }
}

private object EmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() {}

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {}
}
