package dev.fr33zing.launcher.ui.components.node.next

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeState
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeInteractions(
    treeState: TreeState?,
    treeNodeState: TreeNodeState,
    features: NodeRowFeatureSet,
    nodeActions: NodeActions,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onActivatePayload: () -> Unit = {},
    color: Color = LocalNodeAppearance.current.color,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color, longPressable = true)
    val hasFeature =
        remember(features) {
            object {
                val CREATE_ADJACENT = features.contains(NodeRowFeatures.CREATE_ADJACENT)
                val ACTION_BUTTONS = features.contains(NodeRowFeatures.ACTION_BUTTONS)
            }
        }
    val selected = treeNodeState.key == treeState?.selectedKey

    NodeInteractionsLayout {
        Box(
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = {
                    onClearSelectedNode()
                    onActivatePayload()
                },
                onLongClick = onSelectNode
            )
        ) {
            content()
        }

        if (hasFeature.ACTION_BUTTONS)
            AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                NodeActionButtonRow(nodeActions, treeNodeState)
            }
    }
}

@Composable
private fun NodeInteractionsLayout(
    nodeRowIndex: Int = 0,
    content: @Composable () -> Unit,
) {
    Layout(content = content) { measurables, constraints ->
        val nodeRow = measurables[nodeRowIndex].measure(constraints)
        val placeables =
            measurables.mapIndexed { index, measurable ->
                if (index == nodeRowIndex) nodeRow
                else measurable.measure(Constraints.fixed(nodeRow.width, nodeRow.height))
            }
        layout(nodeRow.width, nodeRow.height) {
            placeables.forEach { placeable -> placeable.placeRelative(x = 0, y = 0) }
        }
    }
}
