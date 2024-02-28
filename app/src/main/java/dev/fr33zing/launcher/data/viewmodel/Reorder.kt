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
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.utility.castOrNull
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.state.NodePayloadState
import dev.fr33zing.launcher.data.viewmodel.state.ReferenceFollowingNodePayloadState
import dev.fr33zing.launcher.nodeId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ReorderViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    val childNodeId = savedStateHandle.nodeId()
    var parentNode by mutableStateOf<Node?>(null)
    var reorderableNodes by mutableStateOf<List<ReferenceFollowingNodePayloadState>?>(null)

    init {
        viewModelScope.launch {
            parentNode = db.nodeDao().getParentByChildId(childNodeId).notNull()
            val parentPayload =
                db.getPayloadByNodeId(parentNode!!.kind, parentNode!!.nodeId).notNull(parentNode)

            // Parent node is added as the first element in the list due to a bug with the
            // reorderable modifier implementation which prevents the first element from being
            // animated properly. See here: https://github.com/aclassen/ComposeReorderable#Notes
            val dummyFirstElement =
                listOf(
                    ReferenceFollowingNodePayloadState(
                        NodePayloadState(parentNode!!, parentPayload),
                        null
                    )
                )
            reorderableNodes =
                dummyFirstElement +
                    db.nodeDao().getChildNodes(parentNode!!.nodeId).map { node ->
                        db.getPayloadByNodeId(node.kind, node.nodeId).notNull(node).let { payload ->
                            payload.castOrNull<Reference>()?.targetId?.let { targetId ->
                                ReferenceFollowingNodePayloadState(
                                    underlyingState = NodePayloadState(node, payload),
                                    targetState =
                                        run {
                                            val targetNode =
                                                db.nodeDao().getNodeById(targetId).notNull()
                                            val targetPayload =
                                                db.getPayloadByNodeId(targetNode.kind, targetId)
                                                    .notNull(targetNode)
                                            NodePayloadState(targetNode, targetPayload)
                                        }
                                )
                            }
                                ?: ReferenceFollowingNodePayloadState(
                                    NodePayloadState(node, payload),
                                    null
                                )
                        }
                    }
        }
    }

    fun move(from: Int, to: Int) {
        reorderableNodes =
            reorderableNodes?.toMutableList()?.apply { add(to, removeAt(from)) }
                ?: throw Exception("reorderableNodes is null")
    }

    fun saveChanges() {
        reorderableNodes ?: throw Exception("reorderableNodes is null")

        val fixedNodes =
            reorderableNodes!!
                .subList(1, reorderableNodes!!.size) // Remove dummy first element
                .also { states ->
                    states.forEachIndexed { index, state ->
                        state.underlyingState.node.order = index
                    }
                }
                .map { state -> state.underlyingState.node }

        CoroutineScope(Dispatchers.IO).launch { db.nodeDao().updateMany(fixedNodes) }
    }
}
