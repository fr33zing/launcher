package dev.fr33zing.launcher.ui.pages

import android.app.SearchManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.persistent.Preferences
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
    navigateBack: () -> Unit,
    navigateToTree: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val results by viewModel.resultsFlow.collectAsStateWithLifecycle()
    val history by viewModel.historyFlow.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showRequestFocusButton by remember { mutableStateOf(false) }

    val preferences = Preferences(context)
    val webSearchApplication by preferences.search.webSearchApplication.state

    fun onNavigateBack() {
        showRequestFocusButton = false
        navigateBack()
    }

    fun onNavigateToTree() {
        showRequestFocusButton = false
        navigateToTree()
    }

    fun clearFocus() {
        focusRequester.freeFocus()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun requestFocus() {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun activatePayload(treeNodeState: TreeNodeState) {
        viewModel.activatePayload(context, treeNodeState)
        viewModel.addCurrentQueryToSearchHistory()
        clearFocus()
    }

    fun activateDirectory(treeNodeState: TreeNodeState) {
        viewModel.activateDirectory(treeNodeState)
        clearFocus()
        onNavigateToTree()
    }

    fun onWebSearch() {
        viewModel.addCurrentQueryToSearchHistory()
        Intent()
            .apply {
                action = Intent.ACTION_WEB_SEARCH
                putExtra(SearchManager.QUERY, state.query)
                if (webSearchApplication.isNotBlank()) `package` = webSearchApplication
            }
            .also { context.startActivity(it) }
    }

    LaunchedEffect(Unit) { showRequestFocusButton = true }

    BackHandler(onBack = ::onNavigateBack)

    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        SearchContainer(
            requestFocus = ::requestFocus,
            showRequestFocusButton = showRequestFocusButton,
            controls = {
                SearchBox(
                    query = state.rawQuery,
                    updateQuery = viewModel.updateQuery,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    keyboardController = keyboardController
                ) {
                    results.getOrNull(0)?.let { topResult -> activatePayload(topResult.element) }
                }

                SearchFilters(
                    nodeKindFilter = state.nodeKindFilter,
                    updateFilter = viewModel.updateFilter,
                )
            }
        ) {
            SearchResults(
                query = state.query,
                history = history,
                results = results,
                showHistory = state.query.isBlank(),
                onTapSearchAction = viewModel::addCurrentQueryToSearchHistory,
                onTapHistoricalQuery = {
                    viewModel.updateQuery(it)
                    clearFocus()
                },
                onRemoveHistoricalQuery = viewModel::removeQueryFromSearchHistory,
                onWebSearch = ::onWebSearch,
                onActivateSearchResult = ::activatePayload,
                onActivateDirectorySearchResult = ::activateDirectory
            )
        }
    }
}
