package dev.fr33zing.launcher.data.viewmodel

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
    @OptIn(ExperimentalCoroutinesApi::class)
    val flow: StateFlow<List<TreeNodeState>?> =
        savedStateHandle
            .getStateFlow<String?>("nodeId", null)
            .filterNotNull()
            .transformLatest { nodeIdArgument ->
                val nodeId = nodeIdArgument.toInt()
                val stateHolder = TreeStateHolder(db, nodeId)
                emitAll(stateHolder.flow)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}
