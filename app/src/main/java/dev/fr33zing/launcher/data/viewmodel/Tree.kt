package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeStateHolder
import dev.fr33zing.launcher.data.viewmodel.utility.stagger
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

@HiltViewModel
class TreeViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    private var stateHolder: TreeStateHolder? = null
    private var shouldStaggerFlow = mutableStateOf(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow: StateFlow<List<TreeNodeState>?> =
        savedStateHandle
            .getStateFlow<String?>("nodeId", null)
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { nodeIdArgument ->
                val nodeId = nodeIdArgument.toInt()
                stateHolder = TreeStateHolder(db, nodeId)
                emitAll(stateHolder!!.flow)
            }
            .stagger(shouldStaggerFlow)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun activatePayload(context: Context, treeNodeState: TreeNodeState) {
        stateHolder?.onActivateNode(treeNodeState)
        treeNodeState.nodePayload.payload.activate(db, context)
    }

    fun disableFlowStagger() {
        shouldStaggerFlow.value = false
    }
}
