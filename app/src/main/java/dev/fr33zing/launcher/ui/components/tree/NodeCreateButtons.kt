package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.hasPermission
import dev.fr33zing.launcher.data.persistent.RelativeNodeOffset
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val color = foreground.copy(alpha = 0.5f)
private val icon = Icons.Outlined.Add

enum class NodeCreateButtonPosition(
    val expandFrom: Alignment.Vertical,
) {
    Above(Alignment.Top),
    Below(Alignment.Bottom),
    OutsideBelow(Alignment.Bottom),
}

@Composable
fun NodeCreateButton(
    treeState: TreeState,
    treeNodeState: TreeNodeState,
    adjacentTreeNodeStates: AdjacentTreeNodeStates,
    position: NodeCreateButtonPosition,
    spacing: Dp = LocalNodeDimensions.current.spacing,
    indent: Dp = LocalNodeDimensions.current.indent,
    onClick: (RelativeNodePosition) -> Unit,
) {
    val showChildren =
        remember(treeNodeState.showChildren.value) { treeNodeState.showChildren.value == true }
    val visible =
        remember(
            showChildren,
            position,
            treeState.normalState.selectedKey,
            treeNodeState.key,
            treeNodeState.permissions,
        ) {
            // Only show for the selected node
            if (treeState.normalState.selectedKey != treeNodeState.key) {
                false
            } else {
                fun TreeNodeState.canCreate(scope: PermissionScope) =
                    permissions.hasPermission(PermissionKind.Create, scope)
                val hasPermission =
                    when (position) {
                        NodeCreateButtonPosition.Above -> treeNodeState.canCreate(PermissionScope.Self)
                        NodeCreateButtonPosition.Below ->
                            if (showChildren) {
                                treeNodeState.canCreate(PermissionScope.Recursive)
                            } else {
                                treeNodeState.canCreate(PermissionScope.Self)
                            }
                        NodeCreateButtonPosition.OutsideBelow ->
                            adjacentTreeNodeStates.below?.canCreate(PermissionScope.Self)
                    } ?: false

                // Only show for nodes with the required permissions
                hasPermission &&
                    when (position) {
                        NodeCreateButtonPosition.OutsideBelow ->
                            // Don't show for top level nodes
                            treeNodeState.depth > 0 &&
                                // Only show for nodes that are the last child of their parent
                                treeNodeState.lastChild &&
                                // Don't show for directories with visible children
                                (
                                    treeNodeState.value.node.kind != NodeKind.Directory ||
                                        (
                                            adjacentTreeNodeStates.below?.depth?.let {
                                                it <= treeNodeState.depth
                                            } == true
                                        )
                                )
                        else -> true
                    }
            }
        }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = position.expandFrom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = position.expandFrom) + fadeOut(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val label =
            remember(showChildren, position) {
                when (position) {
                    NodeCreateButtonPosition.Above -> "Add item above"
                    NodeCreateButtonPosition.Below ->
                        if (showChildren) "Add item within" else "Add item below"
                    NodeCreateButtonPosition.OutsideBelow -> "Add item outside"
                }
            }
        val depth =
            remember(showChildren, position, treeNodeState.depth) {
                when (position) {
                    NodeCreateButtonPosition.Above -> treeNodeState.depth
                    NodeCreateButtonPosition.Below ->
                        if (showChildren) treeNodeState.depth + 1 else treeNodeState.depth
                    NodeCreateButtonPosition.OutsideBelow -> treeNodeState.depth - 1
                }
            }
        val onClickFn =
            remember(showChildren, position, treeNodeState.value.node) {
                when (position) {
                    NodeCreateButtonPosition.Above -> {
                        {
                            onClick(
                                RelativeNodePosition(
                                    treeNodeState.underlyingNodeId,
                                    RelativeNodeOffset.Above,
                                ),
                            )
                        }
                    }
                    NodeCreateButtonPosition.Below -> {
                        {
                            onClick(
                                RelativeNodePosition(
                                    treeNodeState.underlyingNodeId,
                                    if (showChildren) {
                                        RelativeNodeOffset.Within
                                    } else {
                                        RelativeNodeOffset.Below
                                    },
                                ),
                            )
                        }
                    }
                    NodeCreateButtonPosition.OutsideBelow -> {
                        {
                            onClick(
                                RelativeNodePosition(
                                    treeNodeState.underlyingNodeParentId
                                        ?: throw Exception("Cannot create outside root node"),
                                    RelativeNodeOffset.Below,
                                ),
                            )
                        }
                    }
                }
            }

        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color)

        NodeDetailContainer(
            depth = depth,
            modifier =
                Modifier.clickable(
                    interactionSource,
                    indication,
                    enabled = visible,
                    onClick = onClickFn,
                ),
        ) {
            NodeDetail(label, color = color, icon = icon, lineThrough = false)
        }
    }
}
