package dev.fr33zing.launcher.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeBrowserStateHolder
import javax.inject.Inject

@HiltViewModel
class MoveViewModel @Inject constructor(private val db: AppDatabase) : ViewModel() {
    val treeBrowser = TreeBrowserStateHolder(db, viewModelScope)

    private var _selectedNodeId: Int? = null
    var selectedNodeId
        private set(value) {
            _selectedNodeId = value
        }
        get() = _selectedNodeId

    private fun onNodeSelected(nodePayload: NodePayloadState) {
        selectedNodeId = nodePayload.node.nodeId
    }
}
