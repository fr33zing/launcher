package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.NodeRow
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.moveToTrash
import dev.fr33zing.launcher.ui.theme.Background
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

    val showDeleteButton = remember {
        row.hasPermission(PermissionKind.Delete, PermissionScope.Self)
    }
    val showMoveButton = remember {
        row.hasPermission(PermissionKind.Move, PermissionScope.Self) ||
            row.hasPermission(PermissionKind.MoveIn, PermissionScope.Self) ||
            row.hasPermission(PermissionKind.MoveOut, PermissionScope.Self)
    }
    val showReorderButton = remember { true }
    val showEditButton = remember { row.hasPermission(PermissionKind.Edit, PermissionScope.Self) }
    val showInfoButton = remember { row.node.kind == NodeKind.Application }

    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        NodeOptionButtonsLayout(
            Modifier.fillMaxHeight()
                .background(Background.copy(alpha = 0.75f))
                .clickable(onClick = { /* Prevent tapping node underneath */})
        ) {
            if (showDeleteButton)
                NodeOptionButton(
                    fontSize,
                    lineHeight,
                    Icons.Outlined.Delete,
                    "Delete",
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        sendNotice(
                            "deleted",
                            "Moved ${row.node.kind.label.lowercase()} '${row.node.label}' to the trash."
                        )
                        CoroutineScope(Dispatchers.IO).launch { db.moveToTrash(row.node) }
                    },
                    onTap = { sendNotice("delete", "Long press to move this item to the trash.") }
                )

            if (showMoveButton)
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.DriveFileMove, "Move") {
                    navController.navigate("move/${row.node.nodeId}")
                }

            if (showReorderButton)
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.SwapVert, "Reorder") {
                    navController.navigate("reorder/${row.node.parentId}")
                }

            if (showEditButton)
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Edit, "Edit") {
                    navController.navigate("edit/${row.node.nodeId}")
                }

            if (showInfoButton)
                NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Info, "Info") {}
        }
    }
}

@Composable
private fun NodeOptionButton(
    fontSize: TextUnit,
    lineHeight: Dp,
    icon: ImageVector,
    text: String,
    onLongPress: () -> Unit = {},
    onTap: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.padding(horizontal = lineHeight * 0.5f).pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            }
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, text, modifier = Modifier.size(lineHeight * 1.15f))
            Text(text, fontSize = fontSize * 0.65f, fontWeight = FontWeight.Bold)
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
