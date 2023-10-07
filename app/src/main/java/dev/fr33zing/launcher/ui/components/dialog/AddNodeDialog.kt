package dev.fr33zing.launcher.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.ui.components.NodeIconAndText

@Composable
fun AddNodeDialog(
    visible: MutableState<Boolean>,
    onDismissRequest: () -> Unit,
    onKindChosen: (NodeKind) -> Unit
) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }
    val kinds = remember {
        listOf(
            NodeKind.Reference,
            NodeKind.Directory,
            NodeKind.Application,
            NodeKind.WebLink,
            NodeKind.File,
            NodeKind.Location,
            NodeKind.Note,
            NodeKind.Checkbox,
            NodeKind.Reminder,
        )
    }

    BaseDialog(
        visible,
        Icons.Filled.Add,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        kinds.forEach { Option(fontSize, lineHeight, it) { onKindChosen(it) } }
    }
}

@Composable
private fun Option(fontSize: TextUnit, lineHeight: Dp, nodeKind: NodeKind, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
