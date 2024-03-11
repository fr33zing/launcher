package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.East
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.hasPermission
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.utility.cast
import dev.fr33zing.launcher.data.utility.castOrNull
import dev.fr33zing.launcher.data.viewmodel.sendJumpToNode
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.blockPointerEvents
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Immutable
class NodeActions(
    val trash: (Int) -> Unit,
    val delete: (Int) -> Unit,
    val move: (Int) -> Unit,
    val reorder: (Int) -> Unit,
    val edit: (Int) -> Unit,
    val create: (Int) -> Unit,
    val viewNote: (Int) -> Unit,
    val beginBatchSelect: () -> Unit,
)

@Immutable
class NodeActionButtonKind(
    val label: String,
    val icon: ImageVector,
    val color: Color = Foreground,
    val visible: (TreeNodeState) -> Boolean,
    val component: @Composable NodeActionButtonKind.(ComponentArguments) -> Unit
) {
    data class ComponentArguments(
        val state: TreeNodeState,
        val actions: NodeActions,
    )

    companion object {
        val kinds =
            listOf(
                // Enter Batch mode
                NodeActionButtonKind(
                    label = "Batch",
                    icon = Icons.Outlined.SelectAll,
                    visible = { true },
                ) { (_, actions) ->
                    ActionButton { actions.beginBatchSelect() }
                },

                // Move node to trash
                NodeActionButtonKind(
                    label = "Trash",
                    icon = Icons.Outlined.Delete,
                    visible = {
                        it.permissions.hasPermission(PermissionKind.Delete, PermissionScope.Self)
                    },
                ) { (state, actions) ->
                    val node = remember(state) { state.value.underlyingState.node }
                    ActionButton {
                        sendNotice(
                            "moved-to-trash:${node.nodeId}",
                            "Moved ${node.kind.label.lowercase()} \"${node.label}\" to the trash."
                        )
                        actions.trash(state.underlyingNodeId)
                    }
                },

                // Empty trash
                NodeActionButtonKind(
                    label = "Empty",
                    icon = Icons.Outlined.DeleteForever,
                    color = Catppuccin.Current.red,
                    visible = { state ->
                        state.value.payload.castOrNull<Directory>()?.specialMode ==
                            Directory.SpecialMode.Trash
                    },
                ) { (state, actions) ->
                    val dialogVisible = remember { mutableStateOf(false) }
                    YesNoDialog(
                        visible = dialogVisible,
                        icon = Icons.Outlined.DeleteForever,
                        yesText = "Delete trash forever",
                        yesColor = Catppuccin.Current.red,
                        yesIcon = Icons.Filled.Dangerous,
                        noText = "Don't empty trash",
                        noIcon = Icons.Filled.ArrowBack,
                        onYes = { actions.delete(state.underlyingNodeId) },
                    )
                    ActionButton { dialogVisible.value = true }
                },

                // Move node
                NodeActionButtonKind(
                    label = "Move",
                    icon = Icons.Outlined.DriveFileMove,
                    visible = { state ->
                        state.permissions.hasPermission(
                            PermissionKind.Move,
                            PermissionScope.Self
                        ) ||
                            state.permissions.hasPermission(
                                PermissionKind.MoveIn,
                                PermissionScope.Self
                            ) ||
                            state.permissions.hasPermission(
                                PermissionKind.MoveOut,
                                PermissionScope.Self
                            )
                    },
                ) { (state, actions) ->
                    ActionButton { actions.move(state.underlyingNodeId) }
                },

                // Reorder nodes
                NodeActionButtonKind(
                    label = "Reorder",
                    icon = Icons.Outlined.SwapVert,
                    visible = { true },
                ) { (state, actions) ->
                    ActionButton { actions.reorder(state.underlyingNodeId) }
                },

                // Edit node
                NodeActionButtonKind(
                    label = "Edit",
                    icon = Icons.Outlined.Edit,
                    visible = { state ->
                        state.permissions.hasPermission(PermissionKind.Edit, PermissionScope.Self)
                    },
                ) { (state, actions) ->
                    ActionButton { actions.edit(state.underlyingNodeId) }
                },

                // Jump to reference target
                NodeActionButtonKind(
                    label = "Jump",
                    icon = Icons.Outlined.East,
                    color = NodeKind.Reference.color,
                    visible = {
                        it.underlyingNodeKind == NodeKind.Reference &&
                            it.value.underlyingState.payload.cast<Reference>().targetId != null
                    },
                ) { (state) ->
                    ActionButton {
                        sendJumpToNode(
                            state.value.underlyingState.payload.cast<Reference>().targetId!!,
                            snap = false
                        )
                    }
                },

                // View application info
                NodeActionButtonKind(
                    label = "Info",
                    icon = Icons.Outlined.Info,
                    visible = { it.value.node.kind == NodeKind.Application },
                ) { (state) ->
                    val context = LocalContext.current
                    ActionButton { state.value.payload.cast<Application>().openInfo(context) }
                }
            )
    }

    @Composable
    private fun ActionButton(action: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val indication =
            rememberCustomIndication(color = color, circular = true, circularSizeFactor = 1.2f)
        val fontSize = LocalNodeDimensions.current.fontSize
        val lineHeight = LocalNodeDimensions.current.lineHeight

        NodeActionButtonLayout(
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = action
            )
        ) {
            Icon(
                icon,
                label,
                tint = color,
                modifier = Modifier.size(lineHeight * 1.15f),
            )
            Text(
                label,
                fontSize = fontSize * 0.65f,
                fontWeight = FontWeight.Bold,
                color = color,
                overflow = TextOverflow.Visible,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NodeActionButtonLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables =
            measurables.map { it.measure(Constraints(maxHeight = constraints.minHeight)) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = constraints.maxWidth / 2 - placeable.width / 2,
                    y = if (index == 0) 0 else constraints.maxHeight - placeable.height
                )
            }
        }
    }
}

@Composable
fun NodeActionButtonRow(
    nodeActions: NodeActions,
    treeNodeState: TreeNodeState,
    fontSize: TextUnit = LocalNodeDimensions.current.fontSize,
    lineHeight: Dp = LocalNodeDimensions.current.lineHeight,
) {
    val visibleActions =
        remember(treeNodeState) { NodeActionButtonKind.kinds.filter { it.visible(treeNodeState) } }
    val componentArguments =
        remember(treeNodeState) {
            NodeActionButtonKind.ComponentArguments(treeNodeState, nodeActions)
        }

    NodeActionButtonRowLayout(
        Modifier.fillMaxHeight().background(Background.copy(alpha = 0.75f)).blockPointerEvents()
    ) {
        visibleActions.forEach { it.component(it, componentArguments) }
    }
}

@Composable
private fun NodeActionButtonRowLayout(
    modifier: Modifier = Modifier,
    lineHeight: Dp = LocalNodeDimensions.current.lineHeight,
    spacing: Dp = LocalNodeDimensions.current.spacing,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val singleLineItemHeight = (spacing + lineHeight).toPx().toInt()
        val square = Constraints.fixed(singleLineItemHeight, singleLineItemHeight)
        val placeables = measurables.map { it.measure(square) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x =
                        (constraints.maxWidth / placeables.size * (index + 0.5f) -
                                placeable.width / 2)
                            .toInt(),
                    y = constraints.maxHeight / 2 - placeable.height / 2
                )
            }
        }
    }
}
