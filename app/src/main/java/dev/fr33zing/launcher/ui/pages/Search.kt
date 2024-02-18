package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.SearchViewModel
import dev.fr33zing.launcher.ui.components.search.SearchBox
import dev.fr33zing.launcher.ui.components.search.SearchContainer
import dev.fr33zing.launcher.ui.components.search.SearchFilters
import dev.fr33zing.launcher.ui.components.search.SearchResults
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Search(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val results by viewModel.resultsFlow.collectAsStateWithLifecycle()
    val history by viewModel.historyFlow.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun clearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        SearchContainer(
            controls = {
                SearchBox(
                    _saveHistory = viewModel::addCurrentQueryToSearchHistory,
                    query = state.query,
                    updateQuery = viewModel.updateQuery,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )

                SearchFilters(
                    nodeKindFilter = state.nodeKindFilter,
                    updateFilter = viewModel.updateFilter,
                )
            }
        ) {
            SearchResults(
                history,
                results,
                showHistory = state.query.isBlank(),
                onTapHistoricalQuery = {
                    viewModel.updateQuery(it)
                    clearFocus()
                }
            )
        }
    }
}
