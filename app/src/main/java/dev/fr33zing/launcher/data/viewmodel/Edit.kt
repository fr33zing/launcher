package dev.fr33zing.launcher.data.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.NodeUpdatedSubject
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val flow =
        savedStateHandle
            .getStateFlow<String?>("nodeId", null)
            .filterNotNull()
            .mapLatest { nodeIdArgument ->
                val nodeId = nodeIdArgument.toInt()
                val node = db.nodeDao().getNodeById(nodeId).notNull()
                val payload = db.getPayloadByNodeId(node.kind, node.nodeId).notNull(node)
                NodePayloadState(node, payload)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun commitChanges(callback: () -> Unit) {
        flow.value?.let { (node, payload) ->
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
}
