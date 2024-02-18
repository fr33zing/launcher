package dev.fr33zing.launcher.ui.components.search

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.data.viewmodel.state.SearchResult

@Composable
fun SearchResults(
    results: List<SearchResult>,
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn {
        if (results.isEmpty()) recentSearches()
        else {
            items(results) {}
        }
    }
}

private fun LazyListScope.recentSearches() {}
