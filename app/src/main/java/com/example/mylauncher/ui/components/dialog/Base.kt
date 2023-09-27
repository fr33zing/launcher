package com.example.mylauncher.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.example.mylauncher.dialogVisible
import com.example.mylauncher.ui.theme.DialogBackground

@Composable
fun BaseDialog(
    visible: MutableState<Boolean>,
    onDismissRequest: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    if (visible.value) {
        dialogVisible.value = true
        Dialog(onDismissRequest = {
            visible.value = false
            dialogVisible.value = false
            onDismissRequest()
        }) {
            (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0.7f)
            content()
        }
    }
}

@Composable
fun BaseDialogCard(content: @Composable () -> Unit = {}) {
    Box(Modifier.clip(RoundedCornerShape(16.dp))) {
        Box(
            Modifier
                .background(DialogBackground)
                .padding(24.dp)
        ) {
            content()
        }
    }
}