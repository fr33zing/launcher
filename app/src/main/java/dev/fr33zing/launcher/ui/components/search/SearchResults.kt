package dev.fr33zing.launcher.ui.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.fr33zing.launcher.data.viewmodel.state.SearchResult
import dev.fr33zing.launcher.ui.components.tree.NodeDetail
import dev.fr33zing.launcher.ui.components.tree.NodeDetailContainer
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

val historyColor = Foreground.mix(Background, 0.5f)

@Composable
fun SearchResults(
    history: List<String>,
    results: List<SearchResult>,
    lazyListState: LazyListState = rememberLazyListState(),
    onTapHistoricalQuery: (String) -> Unit = {},
) {
    LazyColumn {
        if (results.isEmpty()) historyItems(history, onTapHistoricalQuery) else resultItems(results)
    }
}

private fun LazyListScope.historyItems(
    history: List<String>,
    onTapHistoricalQuery: (String) -> Unit
) {
    items(history) { recentSearch ->
        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = historyColor)

        NodeDetailContainer(
            Modifier.clickable(interactionSource, indication) { onTapHistoricalQuery(recentSearch) }
        ) {
            NodeDetail(
                label = recentSearch,
                color = historyColor,
                icon = Icons.Outlined.Search,
                lineThrough = false
            )
        }
    }
}

private fun LazyListScope.resultItems(results: List<SearchResult>) {
    items(results) {}
}
