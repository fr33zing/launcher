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
import dev.fr33zing.launcher.data.persistent.NodeUpdatedSubject
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.state.NodePayloadState
import dev.fr33zing.launcher.nodeId
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EditViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    var nodePayload by mutableStateOf<NodePayloadState?>(null)

    init {
        viewModelScope.launch {
            val nodeId = savedStateHandle.nodeId()
            val node = db.nodeDao().getNodeById(nodeId).notNull()
            val payload = db.getPayloadByNodeId(node.kind, node.nodeId).notNull(node)
            nodePayload = NodePayloadState(node, payload)
        }
    }

    fun commitChanges(callback: () -> Unit) {
        val (node, payload) = nodePayload ?: throw Exception("nodePayload is null")
        viewModelScope.launch {
            db.withTransaction {
                db.update(node)
                db.update(payload)
            }
            NodeUpdatedSubject.onNext(Pair(node.nodeId, node.parentId!!))
            callback()
        }
    }
}
