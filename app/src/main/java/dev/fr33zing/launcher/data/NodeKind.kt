package dev.fr33zing.launcher.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Application as ApplicationPayload
import dev.fr33zing.launcher.data.persistent.payloads.Checkbox as CheckboxPayload
import dev.fr33zing.launcher.data.persistent.payloads.Directory as DirectoryPayload
import dev.fr33zing.launcher.data.persistent.payloads.Location as LocationPayload
import dev.fr33zing.launcher.data.persistent.payloads.Note as NotePayload
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.WebLink as WebLinkPayload
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.mix

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
    Reminder,

    /** Opens system settings to a specific setting group */
    Setting;

    fun implemented(): Boolean =
        when (this) {
            Reference -> true
            Directory -> true
            Application -> true
            WebLink -> true
            Location -> true
            Setting -> true
            Checkbox -> true
            Note -> true
            else -> false
        }

    fun color(payload: Payload? = null, ignoreState: Boolean = false): Color =
        when (this) {
            Reference -> Catppuccin.Current.mauve
            Directory -> {
                if (payload is DirectoryPayload) {
                    if (payload.nodeId == ROOT_NODE_ID) rootDirectoryColor
                    else if (payload.collapsed == true && !ignoreState) collapsedDirectoryColor
                    else directoryColor
                } else directoryColor
            }
            Application -> {
                if (
                    payload is ApplicationPayload &&
                        payload.status != ApplicationPayload.Status.Valid
                ) {
                    Foreground.mix(Background, 0.5f)
                } else Foreground
            }
            Checkbox -> {
                if (!ignoreState && payload is CheckboxPayload && payload.checked)
                    Catppuccin.Current.green.mix(Background, 0.5f)
                else Catppuccin.Current.green
            }
            WebLink -> {
                if (payload is WebLinkPayload && !payload.validUrl) {
                    Catppuccin.Current.yellow.mix(Background, 0.5f)
                } else Catppuccin.Current.yellow
            }
            File -> Catppuccin.Current.peach
            Location -> {
                if (payload is LocationPayload && !payload.status.valid) {
                    Catppuccin.Current.lavender.mix(Background, 0.5f)
                } else Catppuccin.Current.lavender
            }
            Note -> Catppuccin.Current.pink
            Reminder -> Catppuccin.Current.red
            Setting -> Catppuccin.Current.subtext0
        }

    fun lineThrough(payload: Payload? = null, ignoreState: Boolean = false): Boolean =
        when (this) {
            Application -> {
                !ignoreState &&
                    payload is ApplicationPayload &&
                    payload.status != ApplicationPayload.Status.Valid
            }
            Checkbox -> {
                !ignoreState && payload is CheckboxPayload && payload.checked
            }
            WebLink -> {
                !ignoreState && payload is WebLinkPayload && !payload.validUrl
            }
            Location -> {
                !ignoreState && payload is LocationPayload && !payload.status.valid
            }
            else -> false
        }

    fun icon(payload: Payload? = null, ignoreState: Boolean = false): ImageVector =
        when (this) {
            Reference -> Icons.Filled.East
            Directory -> {
                if (payload is DirectoryPayload) {
                    if (payload.specialMode != null) {
                        if (payload.collapsed == true && !ignoreState) {
                            payload.specialMode!!.collapsedIcon ?: payload.specialMode!!.icon
                        } else payload.specialMode!!.icon
                    } else {
                        if (payload.collapsed == true && !ignoreState) Icons.Outlined.Folder
                        else Icons.Filled.Folder
                    }
                } else Icons.Filled.Folder
            }
            Application -> Icons.Filled.Launch
            WebLink -> Icons.Filled.Link
            File -> Icons.Filled.Description
            Location -> Icons.Filled.LocationOn
            Note ->
                if (payload is NotePayload && payload.body.isNotEmpty() && !ignoreState)
                    Icons.Filled.PlaylistAdd
                else Icons.Filled.Notes
            Checkbox ->
                if (payload is CheckboxPayload && payload.checked && !ignoreState)
                    Icons.Filled.CheckBox
                else Icons.Filled.CheckBoxOutlineBlank
            Reminder -> Icons.Filled.Notifications
            Setting -> Icons.Filled.Settings
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
            Setting -> "Setting"
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
