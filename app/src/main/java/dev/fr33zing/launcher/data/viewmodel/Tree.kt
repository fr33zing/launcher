package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeStateHolder
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

@HiltViewModel
class TreeViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    private var stateHolder: TreeStateHolder? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow: StateFlow<List<TreeNodeState>?> =
        savedStateHandle
            .getStateFlow<String?>("nodeId", null)
            .filterNotNull()
            .transformLatest { nodeIdArgument ->
                val nodeId = nodeIdArgument.toInt()
                stateHolder = TreeStateHolder(db, nodeId)
                emitAll(stateHolder!!.flow)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun activatePayload(context: Context, treeNodeState: TreeNodeState) {
        stateHolder?.onActivateNode(treeNodeState)
        treeNodeState.nodePayload.payload.activate(db, context)
    }
}
