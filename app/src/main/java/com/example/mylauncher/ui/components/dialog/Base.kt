package com.example.mylauncher.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.mylauncher.helper.conditional
import com.example.mylauncher.ui.theme.Background
import com.example.mylauncher.ui.theme.Foreground

val baseDialogBorderWidth = 1.dp
val baseDialogBorderColor = Foreground
val baseDialogBackgroundColor = Background.copy(alpha = 0.825f)

@Composable
fun BaseDialog(
    visible: MutableState<Boolean>,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    padding: Boolean = true,
    content: @Composable ColumnScope.(Dp) -> Unit = {},
) {
    if (visible.value) {
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
                modifier = Modifier.padding(vertical = 12.dp).imePadding()
            ) {
                BaseDialogIcon(icon)
                BaseDialogCard(padding, modifier, content)
            }
        }
    }
}

fun Modifier.baseDialogStyles(shape: Shape) =
    this.background(baseDialogBackgroundColor, shape)
        .border(baseDialogBorderWidth, baseDialogBorderColor, shape)

@Composable
private fun BaseDialogCard(
    padding: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable() (ColumnScope.(Dp) -> Unit) = {}
) =
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier.baseDialogStyles(MaterialTheme.shapes.large)
                .conditional(padding) { padding(36.dp) }
                .then(modifier),
    ) {
        content(36.dp)
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
