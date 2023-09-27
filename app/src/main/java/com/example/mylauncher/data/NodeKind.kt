package com.example.mylauncher.data

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import com.example.mylauncher.ui.theme.Catppuccin

private val directoryColor = Catppuccin.Current.sapphire
private val collapsedDirectoryColor = directoryColor.copy(alpha = 0.55f)

enum class NodeKind {
    /** Like a symbolic link */
    Reference,

    /** A list of nodes, not a filesystem directory */
    Directory,

    /** Launches an application */
    Application,

    /** Opens the browser */
    WebLink,

    /** Opens a specific file */
    File,

    /** Opens navigation directions to a specific location */
    Location,

    /** Just some user-editable text */
    Note,

    /** User-toggleable checkbox */
    Checkbox,

    /** A time/date alert, optionally recurring */
    Reminder;

    fun color(collapsed: Boolean = false): Color = when (this) {
        Reference -> Catppuccin.Current.mauve
        Directory -> if (collapsed) collapsedDirectoryColor else directoryColor
        Application -> Color.White
        WebLink -> Catppuccin.Current.yellow
        File -> Catppuccin.Current.peach
        Location -> Catppuccin.Current.lavender
        Note -> Catppuccin.Current.pink
        Checkbox -> Catppuccin.Current.green
        Reminder -> Catppuccin.Current.red
    }

    fun icon(collapsed: Boolean = false): ImageVector = when (this) {
        Reference -> Icons.Filled.East
        Directory -> if (collapsed) Icons.Outlined.Folder else Icons.Filled.Folder
        Application -> Icons.Filled.Launch
        WebLink -> Icons.Filled.Link
        File -> Icons.Filled.Description
        Location -> Icons.Filled.LocationOn
        Note -> Icons.Filled.Notes
        Checkbox -> Icons.Filled.CheckBoxOutlineBlank
        Reminder -> Icons.Filled.Notifications
    }

    fun label(): String = when (this) {
        Reference -> "Reference"
        Directory -> "Directory"
        Application -> "Application"
        WebLink -> "Web link"
        File -> "File opener"
        Location -> "Location"
        Note -> "Text note"
        Checkbox -> "Checkbox"
        Reminder -> "Reminder"
    }

    val color get() = color()
    val icon get() = icon()
    val label get() = label()
}

fun nodeIndent(
    depth: Int,
    indent: Dp,
    lineHeight: Dp,
) = depth * indent + lineHeight / 2f

@Composable
fun nodeLineHeight(): Dp {
    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current
    val fontSize by preferences.getFontSize()
    return with(localDensity) { fontSize.toDp() }
}
