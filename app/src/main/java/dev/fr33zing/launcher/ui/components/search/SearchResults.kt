package dev.fr33zing.launcher.ui.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.fr33zing.launcher.data.viewmodel.state.SearchResult
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.components.tree.NodeDetail
import dev.fr33zing.launcher.ui.components.tree.NodeDetailContainer
import dev.fr33zing.launcher.ui.components.tree.NodeRow
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows

val historyColor = Foreground.mix(Background, 0.5f)

@Composable
fun SearchResults(
    history: List<String>,
    results: List<SearchResult>,
    showHistory: Boolean,
    onTapHistoricalQuery: (String) -> Unit = {},
    onActivateSearchResult: (TreeNodeState) -> Unit = {},
    shadowHeight: Dp = LocalNodeDimensions.current.spacing / 2,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    Box(Modifier.verticalScrollShadows(shadowHeight)) {
        LazyColumn(
            contentPadding = remember { PaddingValues(vertical = shadowHeight) },
        ) {
            if (showHistory) historyItems(history, onTapHistoricalQuery)
            else resultItems(results, onActivateSearchResult)
        }
    }
}

private fun LazyListScope.historyItems(
    history: List<String>,
    onTapHistoricalQuery: (String) -> Unit
) {
    items(history) { recentSearch ->
        val haptics = LocalHapticFeedback.current
        NodeDetailContainer(
            Modifier.clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onTapHistoricalQuery(recentSearch)
            }
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

private fun LazyListScope.resultItems(
    results: List<SearchResult>,
    onActivateSearchResult: (TreeNodeState) -> Unit
) {
    if (results.isEmpty())
        item {
            Text(
                text = "No results.",
                color = historyColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    else
        items(results) { result ->
            NodeRow(
                treeNodeState = result.element,
                features = NodeRowFeatures.Search,
                onActivatePayload = { onActivateSearchResult(result.element) }
            )
        }
}
