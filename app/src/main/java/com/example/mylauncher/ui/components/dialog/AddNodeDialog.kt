package com.example.mylauncher.ui.components.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.persistent.Preferences
import com.example.mylauncher.ui.components.NodeIconAndText

data class NewNodePosition(
    val adjacentIndex: Int,
    val above: Boolean,
)

@Composable
fun AddNodeDialog(visible: MutableState<Boolean>, onDismissRequest: () -> Unit) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }

    BaseDialog(visible, Icons.Filled.Add, onDismissRequest) {
        Option(fontSize, lineHeight, NodeKind.Reference)
        Option(fontSize, lineHeight, NodeKind.Directory)
        Option(fontSize, lineHeight, NodeKind.Application)
        Option(fontSize, lineHeight, NodeKind.WebLink)
        Option(fontSize, lineHeight, NodeKind.File)
        Option(fontSize, lineHeight, NodeKind.Location)
        Option(fontSize, lineHeight, NodeKind.Note)
        Option(fontSize, lineHeight, NodeKind.Checkbox)
        Option(fontSize, lineHeight, NodeKind.Reminder)
    }
}

@Composable
private fun Option(
    fontSize: TextUnit,
    lineHeight: Dp,
    nodeKind: NodeKind,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        NodeIconAndText(
            fontSize = fontSize,
            lineHeight = lineHeight,
            label = nodeKind.label,
            color = nodeKind.color,
            icon = nodeKind.icon,
            softWrap = false,
        )
    }
}
