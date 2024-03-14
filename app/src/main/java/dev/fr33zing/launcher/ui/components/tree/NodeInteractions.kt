package dev.fr33zing.launcher.ui.components.tree

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.dialog.NodeKindPickerDialog
import dev.fr33zing.launcher.ui.components.tree.modal.ModalNodeComponents
import dev.fr33zing.launcher.ui.components.tree.modal.modalNodeContainerModifier
import dev.fr33zing.launcher.ui.components.tree.modal.normalNodeContainerModifier
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalNodeActions
import dev.fr33zing.launcher.ui.components.tree.modal.utility.ModalNodeArguments
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NodeInteractions(
    treeState: TreeState?,
    treeNodeState: TreeNodeState,
    relevance: NodeRelevance?,
    adjacentTreeNodeStates: AdjacentTreeNodeStates?,
    features: NodeRowFeatureSet,
    nodeActions: NodeActions? = null,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onToggleNodeBatchSelected: () -> Unit = {},
    onMoveBatchSelectedNodes: (newParent: TreeNodeState) -> Unit = {},
    onActivatePayload: () -> Unit = {},
    onActivateDirectory: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
    color: Color = LocalNodeAppearance.current.color,
    content: @Composable () -> Unit,
) {
    val hasFeature =
        remember(features) {
            object {
                val EXPAND_DIRECTORIES = features.contains(NodeRowFeatures.RECURSIVE)
                val CREATE_ADJACENT = features.contains(NodeRowFeatures.CREATE_ADJACENT)
                val ACTION_BUTTONS = features.contains(NodeRowFeatures.ACTION_BUTTONS)
                val MODAL = features.contains(NodeRowFeatures.MODAL)
            }
        }
    val selected =
        remember(treeNodeState, treeState) {
            treeNodeState.key == treeState?.normalState?.selectedKey
        }
    val activatePayload by rememberUpdatedState {
        when (treeNodeState.value.node.kind) {
            NodeKind.Directory -> {
                if (hasFeature.EXPAND_DIRECTORIES) onActivatePayload() else onActivateDirectory()
            }
            NodeKind.Note -> {
                CoroutineScope(Dispatchers.Main).launch {
                    nodeActions?.viewNote?.invoke(treeNodeState.value.node.nodeId)
                }
            }
            else -> onActivatePayload()
        }
        Unit
    }
    val treeMode = remember(treeState) { treeState?.mode ?: TreeState.Mode.Normal }
    val modalNodeActions =
        remember { // TODO maybe move this to NodeRow?
            ModalNodeActions(
                activatePayload = activatePayload,
                selectNode = onSelectNode,
                clearSelectedNode = onClearSelectedNode,
                toggleBatchSelected = onToggleNodeBatchSelected,
                moveBatchSelectedNodes = onMoveBatchSelectedNodes,
            )
        }
    val modalArguments =
        remember(treeState) {
            treeState?.let {
                ModalNodeArguments(modalNodeActions, treeState, treeNodeState, relevance ?: NodeRelevance.Relevant)
            }
        }

    BackHandler(enabled = selected) { onClearSelectedNode() }

    @Composable
    fun nodeRow() {
        NodeInteractionsLayout {
            val contentContainerModifier =
                if (modalArguments != null) {
                    Modifier.modalNodeContainerModifier(modalArguments)
                } else {
                    Modifier.normalNodeContainerModifier(
                        treeNodeState,
                        activatePayload,
                        onSelectNode,
                        onClearSelectedNode,
                    )
                }

            if (hasFeature.MODAL && modalArguments != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = contentContainerModifier) {
                    Box(Modifier.weight(1f)) { content() }
                    AnimatedVisibility(
                        visible = treeMode != TreeState.Mode.Normal,
                        enter = fadeIn() + expandHorizontally(expandFrom = AbsoluteAlignment.Right),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = AbsoluteAlignment.Right),
                    ) {
                        ModalNodeComponents(modalArguments)
                    }
                }
            } else {
                Box(contentContainerModifier) { content() }
            }

            if (hasFeature.ACTION_BUTTONS && nodeActions != null) {
                AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                    NodeActionButtonRow(nodeActions, treeNodeState)
                }
            }
        }
    }

    if (!hasFeature.CREATE_ADJACENT || treeState == null || adjacentTreeNodeStates == null) {
        nodeRow()
    } else {
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
                onKindChosen = ::onNewNodeKindChosen,
            )

            NodeCreateButton(
                treeState = treeState,
                treeNodeState = treeNodeState,
                adjacentTreeNodeStates = adjacentTreeNodeStates,
                position = NodeCreateButtonPosition.Above,
                onClick = ::showCreateNodeDialog,
            )

            nodeRow()

            NodeCreateButton(
                treeState = treeState,
                treeNodeState = treeNodeState,
                adjacentTreeNodeStates = adjacentTreeNodeStates,
                position = NodeCreateButtonPosition.Below,
                onClick = ::showCreateNodeDialog,
            )

            NodeCreateButton(
                treeState = treeState,
                treeNodeState = treeNodeState,
                adjacentTreeNodeStates = adjacentTreeNodeStates,
                position = NodeCreateButtonPosition.OutsideBelow,
                onClick = ::showCreateNodeDialog,
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
                if (index == nodeRowIndex) {
                    nodeRow
                } else {
                    measurable.measure(Constraints.fixed(nodeRow.width, nodeRow.height))
                }
            }
        layout(nodeRow.width, nodeRow.height) {
            placeables.forEach { placeable -> placeable.placeRelative(x = 0, y = 0) }
        }
    }
}
