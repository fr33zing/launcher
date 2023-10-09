package dev.fr33zing.launcher.data.persistent.payloads

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity
class Directory(
    payloadId: Int,
    nodeId: Int,
    var specialMode: SpecialMode? = null,
    var collapsed: Boolean? = null,
    var initialVisibility: InitialVisibility = InitialVisibility.Preference
) : Payload(payloadId, nodeId) {

    enum class SpecialMode(
        val modeName: String,
        val defaultDirectoryName: String,
        val icon: ImageVector,
        val collapsedIcon: ImageVector? = null,
        val userCanCreate: Boolean = false,
        val userCanDelete: Boolean = false,
        val userCanRename: Boolean = false,
        val userCanAddWithin: Boolean = false,
    ) {
        NewApplications(
            modeName = "New Applications",
            defaultDirectoryName = "New Applications",
            icon = Icons.Filled.NewReleases,
            collapsedIcon = Icons.Outlined.NewReleases,
            userCanRename = true,
        ),
        Trash(
            modeName = "Trash",
            defaultDirectoryName = "Trash",
            icon = Icons.Filled.Delete,
        ),
    }

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

    override fun activate(db: AppDatabase) {
        collapsed = !(collapsed ?: initiallyCollapsed)
        val payload = this
        CoroutineScope(Dispatchers.IO).launch { db.update(payload) }
    }
}
