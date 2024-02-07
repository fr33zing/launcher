package dev.fr33zing.launcher.ui.components.node.next

import android.util.Log
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.fr33zing.launcher.TAG
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.hasPermission
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private enum class ActionButton(val label: String, val icon: ImageVector) {
    MoveNodeToTrash("Trash", Icons.Outlined.Delete),
    EmptyTrash("Empty", Icons.Outlined.DeleteForever),
    MoveNode("Move", Icons.Outlined.DriveFileMove),
    ReorderNodes("Reorder", Icons.Outlined.SwapVert),
    EditNode("Edit", Icons.Outlined.Edit),
    ViewApplicationInfo("Info", Icons.Outlined.Info)
}

@Composable
fun NodeActionButtonRow(
    treeNodeState: TreeNodeState,
    fontSize: TextUnit = LocalNodeDimensions.current.fontSize,
    lineHeight: Dp = LocalNodeDimensions.current.lineHeight,
) {
    val (node, payload) = treeNodeState.value
    val permissions = treeNodeState.permissions

    val showTrashButton =
        remember(permissions) {
            permissions.hasPermission(PermissionKind.Delete, PermissionScope.Self)
        }
    val showEmptyTrashButton =
        remember(permissions) {
            payload is Directory && payload.specialMode == Directory.SpecialMode.Trash
        }
    val showMoveButton =
        remember(permissions) {
            permissions.hasPermission(PermissionKind.Move, PermissionScope.Self) ||
                permissions.hasPermission(PermissionKind.MoveIn, PermissionScope.Self) ||
                permissions.hasPermission(PermissionKind.MoveOut, PermissionScope.Self)
        }
    val showReorderButton = remember(permissions) { true }
    val showEditButton =
        remember(permissions) {
            permissions.hasPermission(PermissionKind.Edit, PermissionScope.Self)
        }
    val showInfoButton = remember(permissions) { node.kind == NodeKind.Application }

    NodeActionButtonsLayout(
        Modifier.fillMaxHeight()
            .background(Background.copy(alpha = 0.75f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { /* Prevent tapping node underneath */}
            )
    ) {
        if (showTrashButton)
            NodeActionButton(fontSize, lineHeight, Icons.Outlined.Delete, "Trash") {
                sendNotice(
                    "moved-to-trash:${node.nodeId}",
                    "Moved ${node.kind.label.lowercase()} '${node.label}' to the trash."
                )
                // CoroutineScope(Dispatchers.IO).launch { db.moveToTrash(node) }
            }

        if (showEmptyTrashButton) {
            val emptyTrashDialogVisible = remember { mutableStateOf(false) }
            YesNoDialog(
                visible = emptyTrashDialogVisible,
                icon = Icons.Outlined.DeleteForever,
                yesText = "Delete trash forever",
                yesColor = Catppuccin.Current.red,
                yesIcon = Icons.Filled.Dangerous,
                noText = "Don't empty trash",
                noIcon = Icons.Filled.ArrowBack,
                onYes = {
                    Log.d(TAG, "User requested recursive deletion for node: $node")
                    // CoroutineScope(Dispatchers.IO).launch { db.deleteRecursively(node) }
                },
            )
            NodeActionButton(
                fontSize,
                lineHeight,
                Icons.Outlined.DeleteForever,
                "Empty",
                color = Catppuccin.Current.red
            ) {
                emptyTrashDialogVisible.value = true
            }
        }

        if (showMoveButton) {
            NodeActionButton(fontSize, lineHeight, Icons.Outlined.DriveFileMove, "Move") {
                // navController.navigate("move/${node.nodeId}")
            }
        }

        if (showReorderButton) {
            NodeActionButton(fontSize, lineHeight, Icons.Outlined.SwapVert, "Reorder") {
                // navController.navigate("reorder/${node.parentId}")
            }
        }

        if (showEditButton) {
            NodeActionButton(fontSize, lineHeight, Icons.Outlined.Edit, "Edit") {
                // navController.navigate(Routes.Main.editForm(node.nodeId))
            }
        }

        if (showInfoButton) {
            val context = LocalContext.current
            NodeActionButton(fontSize, lineHeight, Icons.Outlined.Info, "Info") {
                // closeNodeOptionsSubject.onNext(Unit)
                (payload as Application).openInfo(context)
            }
        }
    }
}

@Composable
private fun NodeActionButton(
    fontSize: TextUnit,
    lineHeight: Dp,
    icon: ImageVector,
    text: String,
    color: Color = Foreground,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color, circular = true)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.clickable(interactionSource = interactionSource, indication = indication) {
                Log.d(TAG, "Node option button clicked: $text")
                onClick()
            }
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(lineHeight * 0.125f, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.aspectRatio(1f, true)
        ) {
            Icon(
                icon,
                text,
                tint = color,
                modifier = Modifier.size(lineHeight * 1.15f),
            )
            Text(
                text,
                fontSize = fontSize * 0.65f,
                fontWeight = FontWeight.Bold,
                color = color,
                overflow = TextOverflow.Visible,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun NodeActionButtonsLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
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
