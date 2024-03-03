package dev.fr33zing.launcher.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.checkPermission
import dev.fr33zing.launcher.data.persistent.payloads.Note
import dev.fr33zing.launcher.data.utility.cast
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.state.NodePayloadStateHolder
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@HiltViewModel
class ViewNoteViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    data class NoteState(
        val nodeId: Int = -1,
        val title: String = "",
        val body: String = "",
        val created: Date = Date(),
        val updated: Date = Date()
    )

    var hasEditPermission by mutableStateOf(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow =
        savedStateHandle
            .getStateFlow<String?>("nodeId", initialValue = null)
            .filterNotNull()
            .transformLatest {
                val nodeId = it.toInt()
                val node = db.nodeDao().getNodeById(nodeId).notNull()
                val stateHolder = NodePayloadStateHolder(db, node)
                emitAll(stateHolder.flow)
            }
            .mapLatest { (node, payload) ->
                Pair(
                    NoteState(
                        nodeId = node.nodeId,
                        title = node.label.trim(),
                        body = payload.cast<Note>().body.trim(),
                        created = payload.created,
                        updated = payload.updated,
                    ),
                    node
                )
            }
            .onEach { (_, node) ->
                hasEditPermission = false
                CoroutineScope(Dispatchers.IO).launch {
                    hasEditPermission =
                        db.checkPermission(PermissionKind.Edit, PermissionScope.Self, node)
                }
            }
            .mapLatest { (state) -> state }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), NoteState())
}
