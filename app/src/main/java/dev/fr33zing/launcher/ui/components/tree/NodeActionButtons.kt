package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import dev.fr33zing.launcher.data.utility.cast
import dev.fr33zing.launcher.data.utility.castOrNull
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
        val clearSelectedNode: () -> Unit
    )

    companion object {
        val kinds =
            listOf(
                // Move node to trash
                NodeActionButtonKind(
                    label = "Trash",
                    icon = Icons.Outlined.Delete,
                    visible = {
                        it.permissions.hasPermission(PermissionKind.Delete, PermissionScope.Self)
                    },
                ) { (state, actions, clearSelectedNode) ->
                    val node = remember(state) { state.value.underlyingState.node }
                    ActionButton(clearSelectedNode) {
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
                ) { (state, actions, clearSelectedNode) ->
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
                    ActionButton(clearSelectedNode) { dialogVisible.value = true }
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
                ) { (state, actions, clearSelectedNode) ->
                    ActionButton(clearSelectedNode) { actions.move(state.underlyingNodeId) }
                },

                // Reorder nodes
                NodeActionButtonKind(
                    label = "Reorder",
                    icon = Icons.Outlined.SwapVert,
                    visible = { true },
                ) { (state, actions, clearSelectedNode) ->
                    ActionButton(clearSelectedNode) { actions.reorder(state.underlyingNodeId) }
                },

                // Edit node
                NodeActionButtonKind(
                    label = "Edit",
                    icon = Icons.Outlined.Edit,
                    visible = { state ->
                        state.permissions.hasPermission(PermissionKind.Edit, PermissionScope.Self)
                    },
                ) { (state, actions, clearSelectedNode) ->
                    ActionButton(clearSelectedNode) { actions.edit(state.underlyingNodeId) }
                },

                // View application info
                NodeActionButtonKind(
                    label = "Info",
                    icon = Icons.Outlined.Info,
                    visible = { it.value.node.kind == NodeKind.Application },
                ) { (state, _, clearSelectedNode) ->
                    val context = LocalContext.current
                    ActionButton(clearSelectedNode) {
                        state.value.payload.cast<Application>().openInfo(context)
                    }
                }
            )
    }

    @Composable
    private fun ActionButton(clearSelectedNode: () -> Unit, action: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = color, circular = true)
        val fontSize = LocalNodeDimensions.current.fontSize
        val lineHeight = LocalNodeDimensions.current.lineHeight

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = {
                        clearSelectedNode()
                        action()
                    }
                )
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(lineHeight * 0.125f, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.aspectRatio(1f, true)
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
                )
            }
        }
    }
}

@Composable
fun NodeActionButtonRow(
    nodeActions: NodeActions,
    treeNodeState: TreeNodeState,
    onClearSelectedNode: () -> Unit = {},
    fontSize: TextUnit = LocalNodeDimensions.current.fontSize,
    lineHeight: Dp = LocalNodeDimensions.current.lineHeight,
) {
    val visibleActions =
        remember(treeNodeState) { NodeActionButtonKind.kinds.filter { it.visible(treeNodeState) } }
    val componentArguments =
        remember(treeNodeState) {
            NodeActionButtonKind.ComponentArguments(treeNodeState, nodeActions, onClearSelectedNode)
        }

    NodeActionButtonsLayout(
        Modifier.fillMaxHeight().background(Background.copy(alpha = 0.75f)).blockPointerEvents()
    ) {
        visibleActions.forEach { it.component(it, componentArguments) }
    }
}

@Composable
private fun NodeActionButtonsLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val square = Constraints.fixed(constraints.minHeight, constraints.minHeight)
        val placeables = measurables.map { it.measure(square) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x =
                        ((constraints.maxWidth / placeables.size * (index + 0.5f)) -
                                placeable.width / 2)
                            .toInt(),
                    y = 0
                )
            }
        }
    }
}
