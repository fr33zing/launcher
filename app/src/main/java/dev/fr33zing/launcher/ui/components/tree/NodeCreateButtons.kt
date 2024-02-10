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
import dev.fr33zing.launcher.data.persistent.RelativeNodeOffset
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val color = Foreground.copy(alpha = 0.5f)
private val icon = Icons.Outlined.Add

enum class NodeCreateButtonPosition(
    val expandFrom: Alignment.Vertical,
) {
    Above(Alignment.Top),
    Below(Alignment.Bottom),
    Outside(Alignment.Bottom),
}

@Composable
fun NodeCreateButton(
    treeState: TreeState,
    treeNodeState: TreeNodeState,
    position: NodeCreateButtonPosition,
    spacing: Dp = LocalNodeDimensions.current.spacing,
    indent: Dp = LocalNodeDimensions.current.indent,
    onClick: (RelativeNodePosition) -> Unit,
) {
    val visible =
        remember(treeState.selectedKey, treeNodeState.key) {
            val selected = treeState.selectedKey == treeNodeState.key
            if (position == NodeCreateButtonPosition.Outside) selected && treeNodeState.lastChild
            else selected
        }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = position.expandFrom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = position.expandFrom) + fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        val label =
            remember(treeNodeState.showChildren.value, position) {
                when (position) {
                    NodeCreateButtonPosition.Above -> "Add item above"
                    NodeCreateButtonPosition.Below ->
                        if (treeNodeState.showChildren.value == true) "Add item within"
                        else "Add item below"
                    NodeCreateButtonPosition.Outside -> "Add item outside"
                }
            }
        val depth =
            remember(treeNodeState.showChildren.value, treeNodeState.depth, position) {
                when (position) {
                    NodeCreateButtonPosition.Above -> treeNodeState.depth
                    NodeCreateButtonPosition.Below ->
                        if (treeNodeState.showChildren.value == true) treeNodeState.depth + 1
                        else treeNodeState.depth
                    NodeCreateButtonPosition.Outside -> treeNodeState.depth - 1
                }
            }
        val onClickFn =
            remember(treeNodeState.value.node, treeNodeState.showChildren.value, position) {
                when (position) {
                    NodeCreateButtonPosition.Above -> {
                        {
                            onClick(
                                RelativeNodePosition(
                                    treeNodeState.value.node.nodeId,
                                    RelativeNodeOffset.Above
                                )
                            )
                        }
                    }
                    NodeCreateButtonPosition.Below -> {
                        {
                            onClick(
                                RelativeNodePosition(
                                    treeNodeState.value.node.nodeId,
                                    if (treeNodeState.showChildren.value == true)
                                        RelativeNodeOffset.Within
                                    else RelativeNodeOffset.Below
                                )
                            )
                        }
                    }
                    NodeCreateButtonPosition.Outside -> {
                        {
                            onClick(
                                RelativeNodePosition(
                                    treeNodeState.value.node.parentId
                                        ?: throw Exception("Cannot create outside root node"),
                                    RelativeNodeOffset.Below
                                )
                            )
                        }
                    }
                }
            }

        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color)

        NodeDetailContainer(
            depth,
            Modifier.clickable(
                interactionSource,
                indication,
                enabled = visible,
                onClick = onClickFn
            )
        ) {
            NodeDetail(label, color = color, icon = icon, lineThrough = false)
        }
    }
}
