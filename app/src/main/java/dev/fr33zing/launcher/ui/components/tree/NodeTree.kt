package dev.fr33zing.launcher.ui.components.tree

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.utility.unreachable
import dev.fr33zing.launcher.data.viewmodel.ScrollToKeyEvent
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.ActionButton
import dev.fr33zing.launcher.ui.components.ActionButtonSpacing
import dev.fr33zing.launcher.ui.components.ActionButtonVerticalPadding
import dev.fr33zing.launcher.ui.components.tree.modal.ModalActions
import dev.fr33zing.launcher.ui.components.tree.modal.ModalBar
import dev.fr33zing.launcher.ui.components.tree.modal.ModalBarPosition
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.utility.PaddingAndShadowHeight
import dev.fr33zing.launcher.ui.utility.detectFling
import dev.fr33zing.launcher.ui.utility.rememberPaddingAndShadowHeight
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val APPEAR_ANIMATION_DURATION_MS = 350
private const val HIGHLIGHT_ANIMATION_DURATION_MS = 500
private const val UNHIGHLIGHT_ANIMATION_DURATION_MS = 1000
private const val HIGHLIGHT_ANIMATION_MAX_ALPHA = 0.4f

private typealias ListItem = Pair<TreeNodeState, Animatable<Float, AnimationVector1D>?>

@Immutable data class AdjacentTreeNodeStates(val above: TreeNodeState?, val below: TreeNodeState?)

@Composable
fun NodeTree(
    // Flows
    treeStateFlow: Flow<TreeState>,
    treeNodeListFlow: Flow<List<TreeNodeState>>,
    scrollToKeyFlow: Flow<ScrollToKeyEvent?> = flowOf(null),
    highlightKeyFlow: Flow<TreeNodeKey?> = flowOf(null),
    // Regular events
    onSearch: () -> Unit = {},
    onScrollToKeyAfterNextUpdate: () -> Unit = {},
    onScrolledToKey: () -> Unit = {},
    onDisableFlowStagger: () -> Unit = {},
    onActivatePayload: (TreeNodeState) -> Unit = {},
    onSelectNode: (TreeNodeKey) -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onClearHighlightedNode: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
    // Modal events
    onToggleNodeBatchSelected: (TreeNodeKey) -> Unit = {},
    // Scrolling
    setScrollToKeyCallback: ((ScrollToKeyEvent) -> Unit) -> Unit,
    performQueuedScrollToKey: () -> Unit,
    // Other
    paddingAndShadowHeight: PaddingAndShadowHeight = rememberPaddingAndShadowHeight(),
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    nodeActions: NodeActions? = null,
    modalActions: ModalActions? = null,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val (paddingHeight, shadowHeight) = paddingAndShadowHeight
    val coroutineScope = rememberCoroutineScope()
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
            .map { treeNodeList -> treeNodeList.map { Pair(it, animation.progress(it.key)) } }
            .collectAsStateWithLifecycle(emptyList())

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val nodeDimensions = LocalNodeDimensions.current
    val screenHeight =
        remember(configuration, density) {
            with(density) { configuration.screenHeightDp.dp.toPx() }
        }
    val singleLineItemHeight =
        remember(nodeDimensions, density) {
            with(density) { (nodeDimensions.lineHeight + nodeDimensions.spacing).toPx().toInt() }
        }
    val scrollOffset =
        remember(screenHeight, singleLineItemHeight) {
            // TODO maybe consider window insets?
            ((-screenHeight / 2) + singleLineItemHeight).toInt()
        }
    setScrollToKeyCallback { event ->
        val index = treeNodeList.indexOfFirst { (treeNode) -> treeNode.key == event.key }
        if (index != -1) {
            coroutineScope.launch {
                if (event.snap) lazyListState.scrollToItem(index, scrollOffset)
                else lazyListState.animateScrollToItem(index, scrollOffset)
            }
        }
    }

    val highlightAlpha = remember { Animatable(0f) }
    val highlightKey by
        highlightKeyFlow
            .onEach {
                if (it == null) return@onEach
                onDisableFlowStagger()
                coroutineScope.launch {
                    highlightAlpha.snapTo(0f)
                    highlightAlpha.animateTo(
                        HIGHLIGHT_ANIMATION_MAX_ALPHA,
                        tween(HIGHLIGHT_ANIMATION_DURATION_MS)
                    )
                    highlightAlpha.animateTo(0f, tween(UNHIGHLIGHT_ANIMATION_DURATION_MS))
                    onClearHighlightedNode()
                }
            }
            .collectAsStateWithLifecycle(null)

    val canScroll by
        snapshotFlow { lazyListState.canScrollForward || lazyListState.canScrollBackward }
            .onEach { canScroll -> if (canScroll) onDisableFlowStagger() }
            .collectAsStateWithLifecycle(false)

    LaunchedEffect(Unit) { onClearSelectedNode() }

    BackHandler(enabled = treeState.mode != TreeState.Mode.Normal) {
        when (treeState.mode) {
            TreeState.Mode.Normal -> unreachable { "BackHandler should be disabled" }
            TreeState.Mode.Batch -> modalActions?.endBatchSelect?.invoke()
            TreeState.Mode.Move -> modalActions?.endBatchMove?.invoke()
        }
    }

    Column(
        modifier =
            Modifier.fillMaxSize().padding(vertical = paddingHeight).pointerInput(Unit) {
                detectFling(
                    onFirstDown = onDisableFlowStagger,
                    onFlingDown = onSearch,
                    flingUpEnabled = { false },
                    flingDownEnabled = { !lazyListState.canScrollBackward }
                )
            },
    ) {
        @Composable
        fun listItem(
            listItems: List<ListItem>,
            index: Int,
            initialTreeNodeState: TreeNodeState,
            appearAnimationProgress: Animatable<Float, AnimationVector1D>?,
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
                initialTreeNodeState.flow.value.collectAsStateWithLifecycle(initialTreeNodeState)

            @Composable
            fun nodeRow() {
                NodeRow(
                    treeState = treeState,
                    treeNodeState = treeNodeState,
                    adjacentTreeNodeStates = adjacentTreeNodeStates,
                    nodeActions = nodeActions,
                    onSelectNode = { onSelectNode(treeNodeState.key) },
                    onClearSelectedNode = onClearSelectedNode,
                    onToggleNodeMultiSelected = { onToggleNodeBatchSelected(treeNodeState.key) },
                    onActivatePayload = { onActivatePayload(treeNodeState) },
                    onCreateNode = onCreateNode,
                    appearAnimationProgress = appearAnimationProgress,
                )
            }

            if (highlightKey != treeNodeState.key) nodeRow()
            else {
                Box(
                    Modifier.drawWithCache {
                        val highlightColor = treeNodeState.value.node.kind.color
                        onDrawBehind {
                            drawRect(color = highlightColor, alpha = highlightAlpha.value)
                        }
                    }
                ) {
                    nodeRow()
                }
            }
        }

        @Composable
        fun bottomActionButtons() {
            AnimatedVisibility(visible = canScroll, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                        Modifier.fillMaxWidth().padding(vertical = ActionButtonVerticalPadding)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ActionButtonSpacing)) {
                        ActionButton(
                            icon = Icons.Outlined.ArrowUpward,
                            contentDescription = "scroll to top"
                        ) {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        }

                        ActionButton(
                            icon = Icons.Outlined.Search,
                            contentDescription = "scroll to top"
                        ) {
                            onSearch()
                        }
                    }
                }
            }
        }

        //
        // Content
        //

        modalActions?.let { ModalBar(ModalBarPosition.Top, treeState, modalActions) }

        LazyColumn(
            state = lazyListState,
            contentPadding = remember { PaddingValues(vertical = shadowHeight) },
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScrollShadows(shadowHeight)
        ) {
            itemsIndexed(
                items = treeNodeList,
                key = { _, item -> item.first.key },
                contentType = { _, item -> item.first.underlyingNodeKind }
            ) { index, (initialTreeNodeState, appearAnimationProgress) ->
                listItem(treeNodeList, index, initialTreeNodeState, appearAnimationProgress)
                if (index == treeNodeList.lastIndex) bottomActionButtons()
            }

            performQueuedScrollToKey()
        }

        modalActions?.let { ModalBar(ModalBarPosition.Bottom, treeState, modalActions) }
    }
}
