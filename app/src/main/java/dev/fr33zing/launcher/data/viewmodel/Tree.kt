package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeStateHolder
import dev.fr33zing.launcher.data.viewmodel.utility.stagger
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TreeViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val nodeId =
        savedStateHandle.get<String>("nodeId")?.toInt()
            ?: throw Exception("Invalid SavedStateHandle value for key: nodeId")
    private val stateHolder = TreeStateHolder(db, nodeId)
    private var shouldStaggerFlow = mutableStateOf(true)

    val treeNodeListFlow =
        stateHolder.listFlow
            .stagger(shouldStaggerFlow)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val treeStateFlow =
        stateHolder.stateFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TreeState())

    fun activatePayload(context: Context, treeNodeState: TreeNodeState) {
        stateHolder.onActivateNode(treeNodeState)
        treeNodeState.value.payload.activate(db, context)
    }

    fun selectNode(key: TreeNodeKey) {
        stateHolder.onSelectNode(key)
    }

    fun disableFlowStagger() {
        shouldStaggerFlow.value = false
    }
}
