package dev.fr33zing.launcher.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.NodeUpdatedSubject
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.nodeId
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EditViewModel
@Inject
constructor(private val db: AppDatabase, private val savedStateHandle: SavedStateHandle) :
    ViewModel() {
    var node by mutableStateOf<Node?>(null)
    var payload by mutableStateOf<Payload?>(null)

    init {
        viewModelScope.launch {
            val nodeId = savedStateHandle.nodeId()
            node = db.nodeDao().getNodeById(nodeId).notNull()
            payload = db.getPayloadByNodeId(node!!.kind, node!!.nodeId).notNull(node)
        }
    }

    fun commitChanges(callback: () -> Unit) {
        viewModelScope.launch {
            db.withTransaction {
                db.update(node.notNull())
                db.update(payload.notNull(node))
            }
            NodeUpdatedSubject.onNext(Pair(node!!.nodeId, node!!.parentId!!))
            callback()
        }
    }
}
