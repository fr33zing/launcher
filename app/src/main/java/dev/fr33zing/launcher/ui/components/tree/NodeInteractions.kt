package dev.fr33zing.launcher.ui.components.tree

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.dialog.NodeKindPickerDialog
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.deemphasize
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val checkboxPadding = 16.dp
private val checkboxSpacing = 8.dp
private val indeterminateCheckboxColor = Foreground.dim(0.6f)
private val multiSelectedColor = Foreground.dim(0.8f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeInteractions(
    treeState: TreeState?,
    treeNodeState: TreeNodeState,
    adjacentTreeNodeStates: AdjacentTreeNodeStates?,
    features: NodeRowFeatureSet,
    nodeActions: NodeActions? = null,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onToggleNodeMultiSelected: () -> Unit = {},
    onActivatePayload: () -> Unit = {},
    onActivateDirectory: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
    color: Color = LocalNodeAppearance.current.color,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color, longPressable = true)
    val hasFeature =
        remember(features) {
            object {
                val ACTIVATE = features.contains(NodeRowFeatures.ACTIVATE)
                val EXPAND_DIRECTORIES = features.contains(NodeRowFeatures.RECURSIVE)
                val CREATE_ADJACENT = features.contains(NodeRowFeatures.CREATE_ADJACENT)
                val ACTION_BUTTONS = features.contains(NodeRowFeatures.ACTION_BUTTONS)
                val MULTI_SELECT = features.contains(NodeRowFeatures.MULTI_SELECT)
            }
        }
    val selected =
        remember(treeNodeState, treeState) {
            treeNodeState.key == treeState?.normalState?.selectedKey
        }
    val treeMode = remember(treeState) { treeState?.mode ?: TreeState.Mode.Normal }
    val canMultiSelect =
        remember(treeState, treeNodeState) {
            if (!hasFeature.MULTI_SELECT || treeMode != TreeState.Mode.Batch) return@remember false
            val nodeParentId = treeNodeState.value.underlyingState.node.parentId
            val multiSelectParentId = treeState?.batchState?.parentId
            nodeParentId != null &&
                multiSelectParentId != null &&
                nodeParentId == multiSelectParentId
        }
    val multiSelected =
        remember(treeState, treeNodeState) {
            hasFeature.MULTI_SELECT &&
                treeState?.batchState?.selectedKeys?.getOrDefault(treeNodeState.key, false) ?: false
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

            val contentContainerModifier =
                Modifier.conditional(hasFeature.ACTIVATE && treeMode != TreeState.Mode.Batch) {
                        combinedClickable(
                            interactionSource = interactionSource,
                            indication = indication,
                            onClick = {
                                onClearSelectedNode()

                                if (!requireDoubleTapToActivate) activatePayload()
                                else
                                    sendNotice(
                                        "double-tap-to-activate-node",
                                        requireDoubleTapToActivateMessage
                                            ?: throw Exception(
                                                "requireDoubleTapToActivateMessage is null"
                                            )
                                    )
                            },
                            onDoubleClick =
                                if (requireDoubleTapToActivate) activatePayload else null,
                            onLongClick = onSelectNode
                        )
                    }
                    .conditional(treeMode == TreeState.Mode.Batch) {
                        conditional(canMultiSelect) {
                                clickable(interactionSource, indication = null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleNodeMultiSelected()
                                    }
                                    .conditional(multiSelected) { background(multiSelectedColor) }
                            }
                            .conditional(!canMultiSelect) { deemphasize() }
                    }

            if (!hasFeature.MULTI_SELECT) Box(contentContainerModifier) { content() }
            else
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = contentContainerModifier
                ) {
                    Box(Modifier.weight(1f)) { content() }
                    AnimatedVisibility(
                        visible = treeMode == TreeState.Mode.Batch,
                        enter = fadeIn() + expandHorizontally(expandFrom = AbsoluteAlignment.Right),
                        exit =
                            fadeOut() + shrinkHorizontally(shrinkTowards = AbsoluteAlignment.Right)
                    ) {
                        MultiSelectCheckbox(canMultiSelect, multiSelected)
                    }
                }

            if (hasFeature.ACTION_BUTTONS && nodeActions != null)
                AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                    NodeActionButtonRow(nodeActions, treeNodeState)
                }
        }
    }

    if (!hasFeature.CREATE_ADJACENT || treeState == null || adjacentTreeNodeStates == null)
        nodeRow()
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

@Composable
private fun MultiSelectCheckbox(canMultiSelect: Boolean, multiSelected: Boolean) {
    // Keep state during hide animation
    var canMultiSelectState by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { canMultiSelectState = canMultiSelect }

    val modifier = Modifier.padding(horizontal = checkboxPadding)
    if (!canMultiSelectState)
        Icon(
            Icons.Filled.IndeterminateCheckBox,
            contentDescription = "indeterminate checkbox",
            tint = indeterminateCheckboxColor,
            modifier = modifier
        )
    else if (!multiSelected)
        Icon(
            Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = "unchecked checkbox",
            modifier = modifier
        )
    else Icon(Icons.Filled.CheckBox, contentDescription = "checked checkbox", modifier = modifier)
}
