package dev.fr33zing.launcher.ui.components.search

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.state.SearchResult
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.components.tree.NodeDetail
import dev.fr33zing.launcher.ui.components.tree.NodeDetailContainer
import dev.fr33zing.launcher.ui.components.tree.NodeRow
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows

val historyColor = Foreground.mix(Background, 0.5f)
val removeHistoryColor = Catppuccin.Current.red.mix(Background, 0.25f)
const val MAX_SEARCH_RESULTS = 50

@Immutable
private class SearchAction(
    val name: String,
    val icon: ImageVector,
    val block: Arguments.(query: String) -> Result?,
) {
    data class Arguments(
        val context: Context,
        val onWebSearch: () -> Unit,
        val timerApplication: String?,
    )

    data class Result(val text: String, val onClick: (() -> Unit)? = null)

    companion object {
        val kinds =
            listOf(
                SearchAction(
                    name = "Web search",
                    icon = Icons.Outlined.Search,
                ) {
                    Result("Search the web...", onWebSearch)
                },
                SearchAction(
                    name = "Set timer",
                    icon = Icons.Outlined.Timer,
                ) { query ->
                    val timerParts = query.split(' ', limit = 2)
                    val durationParts =
                        timerParts[0].trim(':').split(':').map {
                            it.toIntOrNull() ?: return@SearchAction null
                        }
                    var hours = 0
                    var minutes = 0
                    var seconds = 0

                    when (durationParts.size) {
                        1 -> seconds = durationParts[0]
                        2 -> {
                            minutes = durationParts[0]
                            seconds = durationParts[1]
                        }
                        3 -> {
                            hours = durationParts[0]
                            minutes = durationParts[1]
                            seconds = durationParts[2]
                        }
                        else -> return@SearchAction null
                    }

                    val totalSeconds = seconds + (minutes * 60) + (hours * 3600)

                    while (seconds >= 60) {
                        seconds -= 60
                        minutes++
                    }
                    while (minutes >= 60) {
                        minutes -= 60
                        hours++
                    }

                    val durationText =
                        listOf(Pair(hours, "h"), Pair(minutes, "m"), Pair(seconds, "s"))
                            .filter { it.first > 0 }
                            .joinToString(separator = " ") { "${it.first}${it.second}" }
                    val text = buildString {
                        append("Set timer: ")
                        append(durationText)
                        timerParts.getOrNull(1)?.let { timerLabel ->
                            append(", \"")
                            append(timerLabel)
                            append("\"")
                        }
                    }

                    Result(text) {
                        Intent(AlarmClock.ACTION_SET_TIMER)
                            .apply {
                                putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
                                timerParts.getOrNull(1)?.let { timerLabel ->
                                    putExtra(AlarmClock.EXTRA_MESSAGE, timerLabel)
                                }
                                if (timerApplication?.isNotBlank() == true)
                                    `package` = timerApplication
                            }
                            .also { context.startActivity(it) }
                    }
                },
            )
    }

    @Composable
    fun SearchActionComponent(result: Result, onTapSearchAction: () -> Unit) {
        val haptics = LocalHapticFeedback.current
        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = historyColor)

        NodeDetailContainer(
            Modifier.conditional(result.onClick != null) {
                clickable(interactionSource, indication) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTapSearchAction()
                    result.onClick!!()
                }
            }
        ) {
            NodeDetail(
                label = result.text,
                color = historyColor,
                icon = icon,
                lineThrough = false,
            )
        }
    }
}

@Composable
fun SearchResults(
    query: String,
    history: List<String>,
    results: List<SearchResult>,
    showHistory: Boolean,
    onTapSearchAction: () -> Unit = {},
    onTapHistoricalQuery: (String) -> Unit = {},
    onRemoveHistoricalQuery: (String) -> Unit = {},
    onWebSearch: () -> Unit = {},
    onActivateSearchResult: (TreeNodeState) -> Unit = {},
    onActivateDirectorySearchResult: (TreeNodeState) -> Unit = {},
    shadowHeight: Dp = LocalNodeDimensions.current.spacing / 2,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current
    val preferences = Preferences(context)
    val timerApplication by preferences.search.timerApplication.state
    val searchActionArguments =
        remember(timerApplication) {
            SearchAction.Arguments(context, onWebSearch, timerApplication)
        }

    Box(Modifier.verticalScrollShadows(shadowHeight)) {
        LazyColumn(
            contentPadding = remember { PaddingValues(vertical = shadowHeight) },
        ) {
            if (showHistory) {
                historyItems(history, onTapHistoricalQuery, onRemoveHistoricalQuery)
            } else {
                actionItems(query, searchActionArguments, onTapSearchAction)
                resultItems(query, results, onActivateSearchResult, onActivateDirectorySearchResult)
            }
        }
    }
}

private fun LazyListScope.historyItems(
    history: List<String>,
    onTapHistoricalQuery: (String) -> Unit,
    onRemoveHistoricalQuery: (String) -> Unit,
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
                lineThrough = false,
                textModifier = Modifier.weight(1f)
            )

            val interactionSource = remember { MutableInteractionSource() }
            Icon(
                Icons.Rounded.Close,
                contentDescription = "remove from search history",
                tint = removeHistoryColor,
                modifier =
                    Modifier.clickable(interactionSource, indication = null) {
                        onRemoveHistoricalQuery(recentSearch)
                    },
            )
        }
    }
}

private fun LazyListScope.actionItems(
    query: String,
    arguments: SearchAction.Arguments,
    onTapSearchAction: () -> Unit,
) {
    SearchAction.kinds.forEach { dynamicSearchResult ->
        dynamicSearchResult.block(arguments, query)?.let { result ->
            item(key = dynamicSearchResult.name) {
                dynamicSearchResult.SearchActionComponent(result, onTapSearchAction)
            }
        }
    }
}

private fun LazyListScope.resultItems(
    query: String,
    results: List<SearchResult>,
    onActivateSearchResult: (TreeNodeState) -> Unit,
    onActivateDirectorySearchResult: (TreeNodeState) -> Unit,
) {
    if (results.isEmpty())
        item {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible, enter = fadeIn(tween(1000))) {
                Text(
                    text = "No results.",
                    color = historyColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().absolutePadding(top = 16.dp)
                )
            }
        }
    else
        itemsIndexed(results, key = { _, result -> Pair(result.element, query) }) { index, result ->
            if (index >= MAX_SEARCH_RESULTS) return@itemsIndexed

            val color = remember(result) { result.element.value.node.kind.color }
            NodeRow(
                treeNodeState = result.element,
                features = NodeRowFeatures.Search,
                onActivatePayload = { onActivateSearchResult(result.element) },
                onActivateDirectory = { onActivateDirectorySearchResult(result.element) },
                buildLabelString = {
                    result.substrings.forEach {
                        withStyle(
                            SpanStyle(
                                background =
                                    if (it.matches) color.copy(alpha = 0.25f)
                                    else Color.Transparent,
                            )
                        ) {
                            append(it.text)
                        }
                    }
                },
                textModifier = { Modifier.weight(1f) },
                textEndContent =
                    if (index > 0) null
                    else {
                        {
                            Icon(
                                Icons.Outlined.KeyboardReturn,
                                "keyboard return symbol",
                                tint = historyColor
                            )
                        }
                    }
            )
        }
}
