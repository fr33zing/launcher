package dev.fr33zing.launcher.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground

val baseDialogExtraVerticalPadding = 24.dp
val baseDialogPadding = 24.dp
val baseDialogBorderWidth = 1.dp
val baseDialogBorderColor = Foreground
val baseDialogBackgroundColor = Background.copy(alpha = 0.825f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaseDialog(
    visible: MutableState<Boolean>,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.(Dp) -> Unit = {},
) {
    // HACK: Fix bug caused by hiding dialog when keyboard is visible.
    var reallyVisible by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(visible.value) {
        if (visible.value) reallyVisible = true else if (!imeVisible) reallyVisible = false
    }
    LaunchedEffect(imeVisible) { if (!visible.value && !imeVisible) reallyVisible = false }
    // See below for the rest of the hack.

    if (reallyVisible) {
        Dialog(
            properties = DialogProperties(decorFitsSystemWindows = false),
            onDismissRequest = {
                visible.value = false
                onDismissRequest()
            }
        ) {
            (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0.7f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = baseDialogExtraVerticalPadding).imePadding()
            ) {
                BaseDialogIcon(icon)
                BaseDialogCard(modifier, content)
            }

            // The rest of the hack:
            val focusManager = LocalFocusManager.current
            LaunchedEffect(visible.value) {
                if (imeVisible && !visible.value) focusManager.clearFocus()
            }
        }
    }
}

fun Modifier.baseDialogStyles(shape: Shape) =
    this.background(baseDialogBackgroundColor, shape)
        .border(baseDialogBorderWidth, baseDialogBorderColor, shape)
        .clip(shape)

@Composable
private fun BaseDialogCard(
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.(Dp) -> Unit) = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.baseDialogStyles(MaterialTheme.shapes.large).then(modifier),
    ) {
        content(baseDialogPadding)
    }
}

@Composable
private fun BaseDialogIcon(icon: ImageVector) {
    Icon(
        icon,
        contentDescription = "plus symbol",
        tint = Foreground,
        modifier = Modifier.baseDialogStyles(CircleShape).padding(12.dp).size(32.dp)
    )

    Spacer(Modifier.height(28.dp))
}
