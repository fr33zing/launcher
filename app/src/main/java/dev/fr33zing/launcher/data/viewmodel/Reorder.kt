package dev.fr33zing.launcher.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.nodeId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ReorderViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    var parentNode by mutableStateOf<Node?>(null)
    var reorderableNodes by mutableStateOf<List<Pair<Node, Payload>>?>(null)

    init {
        viewModelScope.launch {
            val nodeToReorder = db.nodeDao().getNodeById(savedStateHandle.nodeId()).notNull()
            parentNode = db.nodeDao().getNodeById(nodeToReorder.parentId ?: ROOT_NODE_ID).notNull()
            val parentPayload =
                db.getPayloadByNodeId(parentNode!!.kind, parentNode!!.nodeId).notNull(parentNode)

            // Parent node is added as the first element in the list due to a bug with the
            // reorderable modifier implementation which prevents the first element from being
            // animated properly. See here: https://github.com/aclassen/ComposeReorderable#Notes
            val dummyFirstElement = listOf(Pair(parentNode!!, parentPayload))
            reorderableNodes =
                dummyFirstElement +
                    db.nodeDao().getChildNodes(parentNode!!.nodeId).map { node ->
                        val payload = db.getPayloadByNodeId(node.kind, node.nodeId).notNull(node)
                        Pair(node, payload)
                    }
        }
    }

    fun move(from: Int, to: Int) {
        reorderableNodes =
            reorderableNodes?.toMutableList()?.apply { add(to, removeAt(from)) }
                ?: throw Exception("reorderableNodes is null")
    }

    fun saveChanges() {
        if (reorderableNodes == null) throw Exception("reorderableNodes is null")

        val fixedNodes =
            reorderableNodes!!
                .subList(1, reorderableNodes!!.size) // Remove dummy first element
                .also { it.forEachIndexed { index, (node) -> node.order = index } }
                .map { it.first }

        CoroutineScope(Dispatchers.IO).launch { db.nodeDao().updateMany(fixedNodes) }
    }
}
