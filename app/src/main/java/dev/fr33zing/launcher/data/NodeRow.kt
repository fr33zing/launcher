package dev.fr33zing.launcher.data

import androidx.compose.runtime.mutableStateOf
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NodeRow(
    val db: AppDatabase,
    val node: Node,
    val payload: Payload,
    val parent: NodeRow?,
    var depth: Int,
) {
    var collapsed: Boolean
        get() = _collapsed.value || parent?.collapsed ?: false
        set(value) {
            if (payload is Directory) {
                payload.collapsed = value
                _collapsed.value = value

                if (payload.initialState == Directory.InitialState.Remember)
                    CoroutineScope(Dispatchers.IO).launch { db.update(payload) }
            }
        }

    private val _collapsed =
        mutableStateOf(if (payload is Directory) payload.initiallyCollapsed else false)
}
