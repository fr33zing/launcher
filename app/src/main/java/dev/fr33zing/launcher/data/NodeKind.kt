package dev.fr33zing.launcher.data

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix

private val directoryColor = Catppuccin.Current.sapphire
private val rootDirectoryColor = Foreground.mix(Background, 0.5f)
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

    fun color(payload: Payload? = null): Color =
        when (this) {
            Reference -> Catppuccin.Current.mauve
            Directory -> {
                if (payload is dev.fr33zing.launcher.data.persistent.payloads.Directory) {
                    if (payload.nodeId == ROOT_NODE_ID) rootDirectoryColor
                    else if (payload.collapsed == true) collapsedDirectoryColor else directoryColor
                } else directoryColor
            }
            Application -> Foreground
            WebLink -> Catppuccin.Current.yellow
            File -> Catppuccin.Current.peach
            Location -> Catppuccin.Current.lavender
            Note -> Catppuccin.Current.pink
            Checkbox -> Catppuccin.Current.green
            Reminder -> Catppuccin.Current.red
        }

    fun icon(payload: Payload? = null): ImageVector =
        when (this) {
            Reference -> Icons.Filled.East
            Directory -> {
                if (payload is dev.fr33zing.launcher.data.persistent.payloads.Directory) {
                    if (payload.specialMode != null) {
                        if (payload.collapsed == true) {
                            payload.specialMode!!.collapsedIcon ?: payload.specialMode!!.icon
                        } else payload.specialMode!!.icon
                    } else {
                        if (payload.collapsed == true) Icons.Outlined.Folder
                        else Icons.Filled.Folder
                    }
                } else Icons.Filled.Folder
            }
            Application -> Icons.Filled.Launch
            WebLink -> Icons.Filled.Link
            File -> Icons.Filled.Description
            Location -> Icons.Filled.LocationOn
            Note -> Icons.Filled.Notes
            Checkbox -> Icons.Filled.CheckBoxOutlineBlank
            Reminder -> Icons.Filled.Notifications
        }

    fun label(): String =
        when (this) {
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

    val color
        get() = color()

    val icon
        get() = icon()

    val label
        get() = label()
}

fun nodeIndent(
    depth: Int,
    indent: Dp,
    lineHeight: Dp,
) = depth * indent + lineHeight / 2f
