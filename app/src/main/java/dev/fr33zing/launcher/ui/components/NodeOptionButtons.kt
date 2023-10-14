package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.NodeRow
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.deleteRecursively
import dev.fr33zing.launcher.data.persistent.moveToTrash
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.rememberCustomIndication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NodeOptionButtons(
    db: AppDatabase,
    navController: NavController,
    visible: Boolean,
    fontSize: TextUnit,
    lineHeight: Dp,
    row: NodeRow,
) {
    val haptics = LocalHapticFeedback.current

    val showTrashButton =
        remember(row) { row.hasPermission(PermissionKind.Delete, PermissionScope.Self) }
    val showEmptyTrashButton =
        remember(row) {
            row.payload is Directory && row.payload.specialMode == Directory.SpecialMode.Trash
        }
    val showMoveButton =
        remember(row) {
            row.hasPermission(PermissionKind.Move, PermissionScope.Self) ||
                row.hasPermission(PermissionKind.MoveIn, PermissionScope.Self) ||
                row.hasPermission(PermissionKind.MoveOut, PermissionScope.Self)
        }
    val showReorderButton = remember(row) { true }
    val showEditButton =
        remember(row) { row.hasPermission(PermissionKind.Edit, PermissionScope.Self) }
    val showInfoButton = remember(row) { row.node.kind == NodeKind.Application }

    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        NodeOptionButtonsLayout(
            Modifier.fillMaxHeight()
                .background(Background.copy(alpha = 0.75f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* Prevent tapping node underneath */}
                )
        ) {
            if (showTrashButton)
                NodeOptionButton(
                    fontSize,
                    lineHeight,
                    Icons.Outlined.Delete,
                    "Trash",
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        sendNotice(
                            "deleted",
                            "Moved ${row.node.kind.label.lowercase()} '${row.node.label}' to the trash."
                        )
                        CoroutineScope(Dispatchers.IO).launch { db.moveToTrash(row.node) }
                    },
                    onClick = { sendNotice("delete", "Long press to move this item to the trash.") }
                )

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
                        CoroutineScope(Dispatchers.IO).launch { db.deleteRecursively(row.node) }
                    },
                )
                NodeOptionButton(
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
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.DriveFileMove, "Move") {
                    navController.navigate("move/${row.node.nodeId}")
                }
            }

            if (showReorderButton) {
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.SwapVert, "Reorder") {
                    navController.navigate("reorder/${row.node.parentId}")
                }
            }

            if (showEditButton) {
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Edit, "Edit") {
                    navController.navigate("edit/${row.node.nodeId}")
                }
            }

            if (showInfoButton) {
                val context = LocalContext.current
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Info, "Info") {
                    closeNodeOptions()
                    (row.payload as Application).openInfo(context)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeOptionButton(
    fontSize: TextUnit,
    lineHeight: Dp,
    icon: ImageVector,
    text: String,
    color: Color = Foreground,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color, circular = true)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
                onLongClick = onLongClick
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
private fun NodeOptionButtonsLayout(
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
