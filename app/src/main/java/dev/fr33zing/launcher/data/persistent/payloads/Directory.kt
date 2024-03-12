package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import androidx.annotation.Keep
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
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.utility.castOrNull
import dev.fr33zing.launcher.data.viewmodel.state.NodePayloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO rename "collapsed" to "childrenVisible", invert conditionals, search entire project for
// "collapsed" so comments are fixed too

@Keep
@Entity
class Directory(
    payloadId: Int,
    nodeId: Int,
    var specialMode: SpecialMode? = null,
    var collapsed: Boolean? = null,
    var initialVisibility: InitialVisibility = InitialVisibility.Preference,
) : Payload(payloadId, nodeId) {
    //
    // Permissions
    //

    enum class SpecialMode(
        val defaultDirectoryName: String,
        val icon: ImageVector,
        val collapsedIcon: ImageVector? = null,
        val initiallyCollapsed: Boolean = false,
        val permissions: PermissionMap = mapOf(),
        /**
         * Check if the contents of a special directory are valid. If it returns false, the child is
         * deleted.
         */
        val isChildValid: ((child: NodePayloadState) -> Boolean)? = null,
        /**
         * Check if the special directory is valid. If it returns false, the directory is delete. This
         * runs **after** [isChildValid].
         */
        val isValid: ((children: List<NodePayloadState>) -> Boolean)? = null,
    ) {
        Root(
            defaultDirectoryName = "<Root>",
            icon = Icons.Rounded.DeviceHub,
            permissions = AllPermissions,
        ),
        Home(
            defaultDirectoryName = "Home",
            icon = Icons.Rounded.Home,
            collapsedIcon = Icons.Outlined.Home,
            initiallyCollapsed = true,
            permissions =
                run {
                    val permissions = AllPermissions.clone().toMutableMap()
                    permissions[PermissionKind.Create] =
                        mutableSetOf(PermissionScope.Self, PermissionScope.Recursive)
                    permissions[PermissionKind.Delete] = mutableSetOf(PermissionScope.Recursive)
                    permissions
                },
        ),
        NewApplications(
            defaultDirectoryName = "New Applications",
            icon = Icons.Filled.NewReleases,
            collapsedIcon = Icons.Outlined.NewReleases,
            permissions =
                mapOf(
                    PermissionKind.Create to mutableSetOf(PermissionScope.Self),
                    PermissionKind.Edit to
                        mutableSetOf(PermissionScope.Self, PermissionScope.Recursive),
                    PermissionKind.MoveOut to mutableSetOf(PermissionScope.Recursive),
                ),
            isChildValid = { child ->
                child.payload.castOrNull<Application>()?.let { application ->
                    application.status == Application.Status.Valid
                } ?: false
            },
            isValid = { children -> children.isNotEmpty() },
        ),
        Trash(
            defaultDirectoryName = "Trash",
            icon = Icons.Filled.Delete,
            collapsedIcon = Icons.Outlined.Delete,
            permissions =
                mapOf(
                    PermissionKind.Create to mutableSetOf(PermissionScope.Self),
                    PermissionKind.Edit to mutableSetOf(PermissionScope.Self),
                    PermissionKind.MoveIn to mutableSetOf(PermissionScope.Recursive),
                    PermissionKind.MoveOut to mutableSetOf(PermissionScope.Recursive),
                ),
        ),
    }

    fun hasPermission(
        kind: PermissionKind,
        scope: PermissionScope,
    ): Boolean {
        return (specialMode ?: return true).permissions.hasPermission(kind, scope)
    }

    //
    // Visibility
    //

    enum class InitialVisibility {
        Preference,
        Remember,
        Collapsed,
        Expanded,
        ;

        fun text(): String =
            when (this) {
                Preference -> "Default from preferences (Remember)"
                Remember -> "Remember when toggled"
                Collapsed -> "Always collapsed"
                Expanded -> "Always expanded"
            }
    }

    val initiallyCollapsed: Boolean
        get() =
            when (initialVisibility) {
                InitialVisibility.Preference -> collapsed ?: false // TODO replace with preference
                InitialVisibility.Collapsed -> true
                InitialVisibility.Expanded -> false
                InitialVisibility.Remember -> collapsed ?: false
            }

    override fun preInsert() = preUpdate()

    override fun preUpdate() {
        collapsed =
            if (initialVisibility == InitialVisibility.Remember) collapsed else initiallyCollapsed
    }

    override fun activate(
        db: AppDatabase,
        context: Context,
    ) {
        collapsed = collapsed != true
        let { payload -> CoroutineScope(Dispatchers.IO).launch { db.update(payload) } }
    }
}
