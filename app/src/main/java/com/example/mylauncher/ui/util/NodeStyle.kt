package com.example.mylauncher.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.ui.theme.Catppuccin

val directoryColor = Catppuccin.Current.sapphire
val collapsedDirectoryColor = directoryColor.copy(alpha = 0.55f)

fun nodeColor(nodeKind: NodeKind, collapsed: Boolean): Color = when (nodeKind) {
    NodeKind.Reference -> TODO()
    NodeKind.Directory -> if (collapsed) collapsedDirectoryColor else directoryColor
    NodeKind.App -> Color.White
}

fun nodeIcon(nodeKind: NodeKind, collapsed: Boolean): ImageVector? = when (nodeKind) {
    NodeKind.Reference -> TODO()
    NodeKind.Directory -> if (collapsed) Icons.Outlined.Folder else Icons.Filled.Folder
    NodeKind.App -> null
}

fun nodeIndent(
    depth: Int,
    indent: Dp,
    lineHeight: Dp,
) = depth * indent + lineHeight / 2f