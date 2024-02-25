package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.SearchHistory
import dev.fr33zing.launcher.data.viewmodel.state.SearchState
import dev.fr33zing.launcher.data.viewmodel.state.SearchStateHolder
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SearchViewModel
@Inject
constructor(private val db: AppDatabase, private val searchHistory: SearchHistory) : ViewModel() {
    private val searchStateHolder = SearchStateHolder(db)
    val stateFlow =
        searchStateHolder.stateFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            SearchState()
        )
    val resultsFlow =
        searchStateHolder.resultsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    val updateQuery = searchStateHolder::updateQuery
    val updateFilter = searchStateHolder::updateFilter

    val historyFlow =
        searchHistory.flow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            searchHistory.get()
        )

    fun addCurrentQueryToSearchHistory() {
        val query = searchStateHolder.stateFlow.value.query
        searchHistory.add(query, viewModelScope)
    }

    fun activatePayload(context: Context, treeNodeState: TreeNodeState) {
        addCurrentQueryToSearchHistory()
        treeNodeState.value.payload.activate(db, context)
    }

    fun activateDirectory(treeNodeState: TreeNodeState) {
        addCurrentQueryToSearchHistory()
        sendJumpToNode(treeNodeState.underlyingNodeId)
    }
}
