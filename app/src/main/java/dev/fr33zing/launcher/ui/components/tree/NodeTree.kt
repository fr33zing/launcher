package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val APPEAR_ANIMATION_DURATION_MS = 350
private const val USE_LAZY_COLUMN = true

private typealias ListItem = Pair<TreeNodeState, Animatable<Float, AnimationVector1D>?>

@Immutable data class AdjacentTreeNodeStates(val above: TreeNodeState?, val below: TreeNodeState?)

@Composable
fun NodeTree(
    treeStateFlow: Flow<TreeState>,
    treeNodeListFlow: Flow<List<TreeNodeState>>,
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    onDisableFlowStagger: () -> Unit = {},
    onActivatePayload: (TreeNodeState) -> Unit = {},
    onSelectNode: (TreeNodeKey) -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
    nodeActions: NodeActions? = null,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val coroutineScope = rememberCoroutineScope()
    val (paddingHeight, shadowHeight) = rememberPaddingAndShadowHeight()
    val hasFeature =
        remember(features) {
            object {
                val APPEAR_ANIMATION = features.contains(NodeRowFeatures.APPEAR_ANIMATION)
            }
        }
    val animation =
        object {
            val progressMap = remember {
                mutableStateMapOf<TreeNodeKey, Animatable<Float, AnimationVector1D>>()
            }

            fun progress(key: TreeNodeKey): Animatable<Float, AnimationVector1D>? =
                if (!hasFeature.APPEAR_ANIMATION) null
                else {
                    progressMap.computeIfAbsent(key) {
                        Animatable(0f).also {
                            coroutineScope.launch {
                                it.animateTo(1f, tween(APPEAR_ANIMATION_DURATION_MS))
                            }
                        }
                    }
                }

            fun resetRemovedNodes(treeNodeStates: List<TreeNodeState>) {
                val nextSnapshotKeys = treeNodeStates.map { it.key }
                progressMap
                    .filterKeys { key -> key !in nextSnapshotKeys }
                    .forEach { (nodeId, _) -> progressMap.remove(nodeId) }
            }
        }
    val treeState by treeStateFlow.collectAsStateWithLifecycle(TreeState())
    val treeNodeList by
        treeNodeListFlow
            .onEach(animation::resetRemovedNodes)
            .collectAsStateWithLifecycle(emptyList())

    /** Used to check for scrollable content overflow */
    var containerHeight by remember { mutableStateOf<Int?>(null) }
    var scrollableContentOverflow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onClearSelectedNode() }

    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .onSizeChanged { containerHeight = it.height }
                    .padding(vertical = paddingHeight)
                    .verticalScrollShadows(shadowHeight)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            awaitFirstDown(false)
                            onDisableFlowStagger()
                        }
                    },
        ) {
            @Composable
            fun listItem(
                listItems: List<ListItem>,
                index: Int,
                initialTreeNodeState: TreeNodeState,
                appearAnimationProgress: Animatable<Float, AnimationVector1D>?
            ) {
                val adjacentTreeNodeStates by
                    remember(listItems, index) {
                        derivedStateOf {
                            AdjacentTreeNodeStates(
                                above = listItems.getOrNull(index - 1)?.first,
                                below = listItems.getOrNull(index + 1)?.first,
                            )
                        }
                    }

                val treeNodeState by
                    initialTreeNodeState.flow.value.collectAsStateWithLifecycle(
                        initialTreeNodeState
                    )

                val simpleMode by
                    remember(appearAnimationProgress) {
                        derivedStateOf { (appearAnimationProgress?.value ?: 1f) < 1f }
                    }

                NodeRow(
                    simple = simpleMode,
                    treeState = treeState,
                    treeNodeState = treeNodeState,
                    adjacentTreeNodeStates = adjacentTreeNodeStates,
                    nodeActions = nodeActions,
                    onSelectNode = { onSelectNode(treeNodeState.key) },
                    onClearSelectedNode = onClearSelectedNode,
                    onActivatePayload = { onActivatePayload(treeNodeState) },
                    onCreateNode = onCreateNode,
                    appearAnimationProgress = appearAnimationProgress,
                )
            }

            fun Modifier.disableFlowStaggerOnOverflow() =
                conditional(containerHeight != null && !scrollableContentOverflow) {
                    onSizeChanged {
                        if (it.height >= containerHeight!!) {
                            scrollableContentOverflow = true
                            onDisableFlowStagger()
                        }
                    }
                }

            val listItems: List<ListItem> by
                remember(treeNodeList) {
                    derivedStateOf { treeNodeList.map { Pair(it, animation.progress(it.key)) } }
                }

            if (USE_LAZY_COLUMN) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = remember { PaddingValues(vertical = shadowHeight) },
                    modifier = Modifier.fillMaxSize().disableFlowStaggerOnOverflow()
                ) {
                    itemsIndexed(
                        items = listItems,
                        key = { _, item -> item.first.key },
                        contentType = { _, item -> item.first.underlyingNodeKind }
                    ) { index, (initialTreeNodeState, appearAnimationProgress) ->
                        listItem(listItems, index, initialTreeNodeState, appearAnimationProgress)
                    }
                }
            } else {
                Column(
                    Modifier.padding(vertical = shadowHeight)
                        .verticalScroll(rememberScrollState())
                        .disableFlowStaggerOnOverflow()
                ) {
                    listItems.forEachIndexed {
                        index,
                        (initialTreeNodeState, appearAnimationProgress) ->
                        key(initialTreeNodeState.key) {
                            listItem(
                                listItems,
                                index,
                                initialTreeNodeState,
                                appearAnimationProgress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberPaddingAndShadowHeight(): Pair<Dp, Dp> {
    val density = LocalDensity.current
    val statusBarsTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarsBottom =
        with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val verticalPadding =
        remember(WindowInsets.statusBars, WindowInsets.navigationBars) {
            listOf(statusBarsTop, navigationBarsBottom).max()
        }
    val hiddenRatio = 0.75f
    val shadowRatio = 1f - hiddenRatio
    val paddingHeight = verticalPadding * hiddenRatio
    val shadowHeight = verticalPadding * shadowRatio
    return remember { Pair(paddingHeight, shadowHeight) }
}
