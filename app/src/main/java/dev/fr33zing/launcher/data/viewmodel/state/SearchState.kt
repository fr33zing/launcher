package dev.fr33zing.launcher.data.viewmodel.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import dev.fr33zing.launcher.data.AllPermissions
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.utility.FuzzyMatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@Immutable
data class SearchState(
    val rawQuery: String = "",
    val nodeKindFilter: Map<NodeKind, Boolean> = NodeKind.entries.associateWith { false },
) {
    val query
        get() = rawQuery.trim()

    fun filterPredicate(nodeKind: NodeKind): Boolean =
        nodeKindFilter[nodeKind] ?: nodeKindFilter.all { !it.value }
}

class SearchStateHolder(private val db: AppDatabase) {
    private val treeNodeStateFlows = mutableStateMapOf<Int, Flow<TreeNodeState>>()

    private val _stateFlow = MutableStateFlow(SearchState())
    val stateFlow = _stateFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val resultsFlow: Flow<List<FuzzyMatcher.Result<TreeNodeState>>> =
        db.nodeDao()
            .getAllFlow()
            .distinctUntilChanged()
            .mapLatest { nodes -> FuzzyMatcher(elements = nodes, toStringFn = { it.label }) }
            .let { fuzzyMatcherFlow ->
                stateFlow
                    .combine(fuzzyMatcherFlow) { state, fuzzyMatcher -> Pair(state, fuzzyMatcher) }
                    .mapLatest { (state, fuzzyMatcher) ->
                        fuzzyMatcher.match(state.query) { state.filterPredicate(it.kind) }
                    }
                    .onEach(::removeTreeNodeStateFlowsIfMissingFromResults)
            }
            .flatMapLatest { results ->
                combine(
                    results.map { result ->
                        getTreeNodeStateFlow(result.element).map { treeNodeState ->
                            result.transform { treeNodeState }
                        }
                    }
                ) {
                    it.toList()
                }
            }

    fun updateQuery(query: String) {
        _stateFlow.update { state -> state.copy(rawQuery = query) }
    }

    fun updateFilter(nodeKind: NodeKind, enabled: Boolean) {
        _stateFlow.update { state ->
            state.copy(
                nodeKindFilter =
                    state.nodeKindFilter.toMutableMap().also { map -> map[nodeKind] = enabled }
            )
        }
    }

    private fun removeTreeNodeStateFlowsIfMissingFromResults(
        results: List<FuzzyMatcher.Result<Node>>
    ) {
        treeNodeStateFlows.keys.forEach { key ->
            if (key !in results.map { it.element.nodeId }) treeNodeStateFlows.remove(key)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getTreeNodeStateFlow(
        node: Node,
    ): Flow<TreeNodeState> =
        treeNodeStateFlows.computeIfAbsent(node.nodeId) {
            val parentStateHolder = NodePayloadStateHolder(db, node)

            parentStateHolder.flowWithReferenceTarget.mapLatest { value ->
                val flow = lazy { // TODO reuse current flow somehow
                    getTreeNodeStateFlow(node)
                }

                TreeNodeState(
                    key = TreeNodeKey.topLevelKey(value.node.nodeId),
                    permissions = AllPermissions,
                    value = value,
                    flow = flow,
                )
            }
        }
}
