package dev.fr33zing.launcher.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.ui.components.tree.NodeDetail
import dev.fr33zing.launcher.ui.components.tree.NodeDetailContainer
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun NodeKindPickerDialog(
    visible: MutableState<Boolean>,
    onDismissRequest: () -> Unit,
    onKindChosen: (NodeKind) -> Unit,
) {
    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        val spacing = LocalNodeDimensions.current.spacing
        val kinds = remember { NodeKind.entries }

        BaseDialog(
            visible,
            Icons.Filled.Add,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(IntrinsicSize.Min),
        ) { padding ->
            val verticalPadding =
                remember {
                    padding - spacing / 2
                }
            Column(modifier = Modifier.padding(vertical = verticalPadding)) {
                kinds.forEach { Option(it) { onKindChosen(it) } }
            }
        }
    }
}

@Composable
private fun Option(
    kind: NodeKind,
    onClick: () -> Unit,
) {
    val appearance = rememberNodeAppearance(kind)
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = appearance.color)

    NodeDetailContainer(
        modifier =
            Modifier.clickable(
                interactionSource,
                indication,
                onClick = onClick,
            ),
    ) {
        NodeDetail(label = kind.label, color = appearance.color, icon = appearance.icon)
    }
}
