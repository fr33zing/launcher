package dev.fr33zing.launcher.data.persistent.payloads

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import dev.fr33zing.launcher.data.AllPermissions
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionMap
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.clone
import dev.fr33zing.launcher.data.hasPermission

// TODO rename "collapsed" to "childrenVisible", invert conditionals, search entire project for
// "collapsed" so comments are fixed too

@Entity
class Directory(
    payloadId: Int,
    nodeId: Int,
    var specialMode: SpecialMode? = null,
    var collapsed: Boolean? = null,
    var initialVisibility: InitialVisibility = InitialVisibility.Preference
) : Payload(payloadId, nodeId) {

    //
    // Permissions
    //

    enum class SpecialMode(
        val modeName: String,
        val defaultDirectoryName: String,
        val icon: ImageVector,
        val collapsedIcon: ImageVector? = null,
        val permissions: PermissionMap = mapOf()
    ) {
        Root(
            modeName = "Root",
            defaultDirectoryName = "~",
            icon = Icons.Rounded.DeviceHub,
            permissions = AllPermissions
        ),
        Home(
            modeName = "Home",
            defaultDirectoryName = "Home",
            icon = Icons.Rounded.Home,
            collapsedIcon = Icons.Outlined.Home,
            permissions =
                run {
                    val permissions = AllPermissions.clone().toMutableMap()
                    permissions[PermissionKind.Create] = mutableSetOf(PermissionScope.Recursive)
                    permissions[PermissionKind.Delete] = mutableSetOf(PermissionScope.Recursive)
                    permissions
                }
        ),
        NewApplications(
            modeName = "New Applications",
            defaultDirectoryName = "New Applications",
            icon = Icons.Filled.NewReleases,
            collapsedIcon = Icons.Outlined.NewReleases,
            permissions =
                mapOf(
                    PermissionKind.Edit to
                        mutableSetOf(PermissionScope.Self, PermissionScope.Recursive),
                    PermissionKind.MoveOut to mutableSetOf(PermissionScope.Recursive),
                ),
        ),
        Trash(
            modeName = "Trash",
            defaultDirectoryName = "Trash",
            icon = Icons.Filled.Delete,
            collapsedIcon = Icons.Outlined.Delete,
            permissions =
                mapOf(
                    PermissionKind.Edit to mutableSetOf(PermissionScope.Self),
                    PermissionKind.MoveIn to mutableSetOf(PermissionScope.Recursive),
                    PermissionKind.MoveOut to mutableSetOf(PermissionScope.Recursive),
                ),
        ),
    }

    fun hasPermission(kind: PermissionKind, scope: PermissionScope): Boolean {
        return (specialMode ?: return true).permissions.hasPermission(kind, scope)
    }

    //
    // Visibility
    //

    enum class InitialVisibility {
        Preference,
        Remember,
        Collapsed,
        Expanded;

        fun text(): String =
            when (this) {
                Preference -> "Default from preferences (Expanded)"
                Remember -> "Remember when toggled"
                Collapsed -> "Always collapsed"
                Expanded -> "Always expanded"
            }
    }

    val initiallyCollapsed: Boolean
        get() =
            when (initialVisibility) {
                InitialVisibility.Preference -> false // TODO replace with preference
                InitialVisibility.Collapsed -> true
                InitialVisibility.Expanded -> false
                InitialVisibility.Remember -> collapsed ?: false
            }

    override fun preInsert() = preUpdate()

    override fun preUpdate() {
        collapsed =
            if (initialVisibility == InitialVisibility.Remember) collapsed else initiallyCollapsed
    }
}
