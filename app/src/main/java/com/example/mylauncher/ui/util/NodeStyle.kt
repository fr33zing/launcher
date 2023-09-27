package com.example.mylauncher.ui.util

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.Preferences
import com.example.mylauncher.ui.theme.Catppuccin

val directoryColor = Catppuccin.Current.sapphire
val collapsedDirectoryColor = directoryColor.copy(alpha = 0.55f)

fun nodeColor(nodeKind: NodeKind, collapsed: Boolean = false): Color = when (nodeKind) {
    NodeKind.Reference -> Catppuccin.Current.mauve
    NodeKind.Directory -> if (collapsed) collapsedDirectoryColor else directoryColor
    NodeKind.Application -> Color.White
    NodeKind.WebLink -> Catppuccin.Current.yellow
    NodeKind.File -> Catppuccin.Current.peach
    NodeKind.Location -> Catppuccin.Current.lavender
    NodeKind.Note -> Catppuccin.Current.pink
    NodeKind.Checkbox -> Catppuccin.Current.green
    NodeKind.Reminder -> Catppuccin.Current.red
}

fun nodeIcon(nodeKind: NodeKind, collapsed: Boolean = false): ImageVector = when (nodeKind) {
    NodeKind.Reference -> Icons.Filled.East
    NodeKind.Directory -> if (collapsed) Icons.Outlined.Folder else Icons.Filled.Folder
    NodeKind.Application -> Icons.Filled.Launch
    NodeKind.WebLink -> Icons.Filled.Link
    NodeKind.File -> Icons.Filled.Description
    NodeKind.Location -> Icons.Filled.LocationOn
    NodeKind.Note -> Icons.Filled.Notes
    NodeKind.Checkbox -> Icons.Filled.CheckBoxOutlineBlank
    NodeKind.Reminder -> Icons.Filled.Notifications
}

fun nodeKindName(nodeKind: NodeKind) = when (nodeKind) {
    NodeKind.Reference -> "Reference"
    NodeKind.Directory -> "Directory"
    NodeKind.Application -> "Application"
    NodeKind.WebLink -> "Web link"
    NodeKind.File -> "File opener"
    NodeKind.Location -> "Location"
    NodeKind.Note -> "Text note"
    NodeKind.Checkbox -> "Checkbox"
    NodeKind.Reminder -> "Reminder"
}

fun nodeIndent(
    depth: Int,
    indent: Dp,
    lineHeight: Dp,
) = depth * indent + lineHeight / 2f

@Composable
fun nodeLineHeight(context: Context, density: Density): Dp {
    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current
    val fontSize by preferences.getFontSize()
    return with(localDensity) { fontSize.toDp() }
}