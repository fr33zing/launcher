package dev.fr33zing.launcher.ui.components.tree

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.dialog.NodeKindPickerDialog
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

const val DOUBLE_TAP_DELAY: Long = 300

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeInteractions(
    treeState: TreeState?,
    treeNodeState: TreeNodeState,
    adjacentTreeNodeStates: AdjacentTreeNodeStates,
    features: NodeRowFeatureSet,
    nodeActions: NodeActions,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onActivatePayload: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
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

    BackHandler(enabled = selected) { onClearSelectedNode() }

    @Composable
    fun nodeRow() {
        NodeInteractionsLayout {
            val requireDoubleTapToActivateMessage =
                remember(treeNodeState.value.node.kind) {
                    treeNodeState.value.node.kind.requiresDoubleTapToActivate()
                }
            val requireDoubleTapToActivate =
                remember(requireDoubleTapToActivateMessage) {
                    requireDoubleTapToActivateMessage != null
                }

            Box(
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = {
                        onClearSelectedNode()

                        if (!requireDoubleTapToActivate) onActivatePayload()
                        else
                            sendNotice(
                                "double-tap-to-activate-node",
                                requireDoubleTapToActivateMessage
                                    ?: throw Exception("requireDoubleTapToActivateMessage is null")
                            )
                    },
                    onDoubleClick = if (requireDoubleTapToActivate) onActivatePayload else null,
                    onLongClick = onSelectNode
                )
            ) {
                content()
            }

            if (hasFeature.ACTION_BUTTONS)
                AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                    NodeActionButtonRow(nodeActions, treeNodeState, onClearSelectedNode)
                }
        }
    }

    if (!hasFeature.CREATE_ADJACENT || treeState == null) nodeRow()
    else {
        Column {
            val nodeKindPickerDialogVisible = remember { mutableStateOf(false) }
            var newNodePosition by remember { mutableStateOf<RelativeNodePosition?>(null) }

            fun showCreateNodeDialog(position: RelativeNodePosition) {
                newNodePosition = position
                nodeKindPickerDialogVisible.value = true
            }

            fun onCreateNodeDialogDismissed() {
                newNodePosition = null
                onClearSelectedNode()
            }

            fun onNewNodeKindChosen(kind: NodeKind) =
                newNodePosition?.let { position ->
                    nodeKindPickerDialogVisible.value = false
                    onCreateNode(position, kind)
                    onClearSelectedNode()
                } ?: throw Exception("newNodePosition is null")

            NodeKindPickerDialog(
                visible = nodeKindPickerDialogVisible,
                onDismissRequest = ::onCreateNodeDialogDismissed,
                onKindChosen = ::onNewNodeKindChosen
            )

            NodeCreateButton(
                treeState = treeState,
                treeNodeState = treeNodeState,
                adjacentTreeNodeStates = adjacentTreeNodeStates,
                position = NodeCreateButtonPosition.Above,
                onClick = ::showCreateNodeDialog
            )

            nodeRow()

            NodeCreateButton(
                treeState = treeState,
                treeNodeState = treeNodeState,
                adjacentTreeNodeStates = adjacentTreeNodeStates,
                position = NodeCreateButtonPosition.Below,
                onClick = ::showCreateNodeDialog
            )

            if (treeNodeState.depth > 0)
                NodeCreateButton(
                    treeState = treeState,
                    treeNodeState = treeNodeState,
                    adjacentTreeNodeStates = adjacentTreeNodeStates,
                    position = NodeCreateButtonPosition.OutsideBelow,
                    onClick = ::showCreateNodeDialog
                )
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
