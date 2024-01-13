package dev.fr33zing.launcher.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadListStateHolder
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class HomeUiState(val nodePayloads: List<NodePayloadState> = listOf())

@HiltViewModel
class HomeViewModel @Inject constructor(private val db: AppDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val homeNode = db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home) // async
            val childNodes = db.nodeDao().getChildNodes(homeNode.nodeId) // async
            val stateList = NodePayloadListStateHolder(db, childNodes)
            val uiStateFlow = stateList.flow.map { HomeUiState(it.toList()) }

            _uiState.emitAll(uiStateFlow)
        }
    }
}
