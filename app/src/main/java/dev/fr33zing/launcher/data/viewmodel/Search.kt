package dev.fr33zing.launcher.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.viewmodel.state.SearchState
import dev.fr33zing.launcher.data.viewmodel.state.SearchStateHolder
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SearchViewModel @Inject constructor(private val db: AppDatabase) : ViewModel() {
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
}
