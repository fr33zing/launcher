package com.example.mylauncher.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.example.mylauncher.data.Preferences
import com.example.mylauncher.ui.components.NodeIconAndText
import com.example.mylauncher.ui.theme.DialogBackground

@Composable
fun YesNoDialog(
    visible: MutableState<Boolean>,
    yesText: String,
    yesColor: Color,
    yesIcon: ImageVector,
    noText: String,
    noColor: Color,
    noIcon: ImageVector,
    onDismissRequest: () -> Unit = {},
    onYes: () -> Unit = {},
    onNo: () -> Unit = {},
) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }

    BaseDialog(visible, onDismissRequest) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(CircleShape)) {
                Icon(
                    Icons.Filled.QuestionMark,
                    contentDescription = "question mark",
                    tint = Color.White,
                    modifier = Modifier
                        .background(DialogBackground)
                        .padding(lineHeight * 0.5f)
                        .size(lineHeight * 1.5f)
                )
            }

            Spacer(Modifier.height(lineHeight * 0.85f))

            BaseDialogCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(lineHeight * 0.8f),
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    Option(visible, fontSize, lineHeight, noText, noColor, noIcon, onNo)
                    Option(visible, fontSize, lineHeight, yesText, yesColor, yesIcon, onYes)
                }
            }
        }
    }
}

@Composable
private fun Option(
    visible: MutableState<Boolean>,
    fontSize: TextUnit,
    lineHeight: Dp,
    text: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    visible.value = false
                    onClick()
                })
            }) {
        NodeIconAndText(
            fontSize = fontSize,
            lineHeight = lineHeight,
            label = text,
            color = color,
            icon = icon,
            softWrap = false,
        )
    }
}