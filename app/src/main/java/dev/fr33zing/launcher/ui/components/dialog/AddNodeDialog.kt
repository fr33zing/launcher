package dev.fr33zing.launcher.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.fr33zing.launcher.ui.components.node.NodeIconAndText
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Composable
fun AddNodeDialog(
    visible: MutableState<Boolean>,
    onDismissRequest: () -> Unit,
    onKindChosen: (NodeKind) -> Unit
) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }
    val kinds = remember { NodeKind.values() }

    BaseDialog(
        visible,
        Icons.Filled.Add,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) { padding ->
        val verticalPadding = remember { padding - Preferences.spacingDefault / 2 }
        Column(modifier = Modifier.padding(vertical = verticalPadding)) {
            kinds.forEach { Option(padding, fontSize, lineHeight, it) { onKindChosen(it) } }
        }
    }
}

@Composable
private fun Option(
    padding: Dp,
    fontSize: TextUnit,
    lineHeight: Dp,
    nodeKind: NodeKind,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = nodeKind.color)

    Box(Modifier.clickable(interactionSource, indication, onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = padding, vertical = Preferences.spacingDefault / 2)
        ) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = nodeKind.label,
                color =
                    if (nodeKind.implemented()) nodeKind.color
                    else nodeKind.color.copy(alpha = 0.5f),
                icon = nodeKind.icon,
                softWrap = false,
            )
        }
    }
}
