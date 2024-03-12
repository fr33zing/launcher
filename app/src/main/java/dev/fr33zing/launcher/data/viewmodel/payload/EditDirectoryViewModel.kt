package dev.fr33zing.launcher.data.viewmodel.payload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.nodeId
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditDirectoryViewModel
    @Inject
    constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
        var nodePath by mutableStateOf<List<Node>>(emptyList())

        init {
            viewModelScope.launch {
                val node = db.nodeDao().getNodeById(savedStateHandle.nodeId()).notNull()
                nodePath = db.nodeLineage(node).also { it.removeLastOrNull() }
            }
        }
    }
