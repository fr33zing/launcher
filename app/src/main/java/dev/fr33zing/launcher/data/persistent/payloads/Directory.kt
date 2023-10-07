package dev.fr33zing.launcher.data.persistent.payloads

import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO fix collapse behavior in ui

@Entity
class Directory(
    payloadId: Int,
    nodeId: Int,
    var collapsed: Boolean? = null,
    var initialState: InitialState = InitialState.Preference
) : Payload(payloadId, nodeId) {

    enum class InitialState() {
        Preference,
        Remember,
        Collapsed,
        Expanded;

        fun text(): String =
            when (this) {
                Preference -> "Default from preferences"
                Collapsed -> "Always collapsed"
                Expanded -> "Always expanded"
                Remember -> "Remember when toggled"
            }
    }

    val initiallyCollapsed: Boolean
        get() =
            when (initialState) {
                InitialState.Preference -> false // TODO replace with preference
                InitialState.Collapsed -> true
                InitialState.Expanded -> false
                InitialState.Remember -> collapsed ?: false
            }

    override fun preInsert() = preUpdate()

    override fun preUpdate() {
        collapsed = if (initialState == InitialState.Remember) collapsed else initiallyCollapsed
    }

    override fun activate(db: AppDatabase) {
        collapsed = !(collapsed ?: initiallyCollapsed)
        val payload = this
        CoroutineScope(Dispatchers.IO).launch { db.update(payload) }
    }
}
