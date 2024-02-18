package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.SearchViewModel
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
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
    val context = LocalContext.current
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val results by viewModel.resultsFlow.collectAsStateWithLifecycle()
    val history by viewModel.historyFlow.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun clearFocus() {
        focusRequester.freeFocus()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun activatePayload(treeNodeState: TreeNodeState) {
        viewModel.activatePayload(context, treeNodeState)
        viewModel.addCurrentQueryToSearchHistory()
        clearFocus()
    }

    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        SearchContainer(
            controls = {
                SearchBox(
                    query = state.query,
                    updateQuery = viewModel.updateQuery,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    onGo = {
                        results.getOrNull(0)?.let { topResult ->
                            activatePayload(topResult.element)
                        }
                    }
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
                },
                onActivateSearchResult = {
                    activatePayload(it)
                    clearFocus()
                }
            )
        }
    }
}
