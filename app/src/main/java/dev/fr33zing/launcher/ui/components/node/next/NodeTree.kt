package dev.fr33zing.launcher.ui.components.node.next

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeState
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.node.next.utility.createLocalNodeDimensions
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val APPEAR_ANIMATION_DURATION_MS = 350

@Composable
fun NodeTree(
    treeStateFlow: Flow<TreeState>,
    treeNodeListFlow: Flow<List<TreeNodeState>>,
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    onDisableFlowStagger: () -> Unit = {},
    onActivatePayload: (TreeNodeState) -> Unit = {},
    onSelectNode: (TreeNodeKey) -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState()
) {
    val coroutineScope = rememberCoroutineScope()
    val (paddingHeight, shadowHeight) = paddingAndShadowHeight()
    val hasFeature =
        remember(features) {
            object {
                val APPEAR_ANIMATION = features.contains(NodeRowFeatures.APPEAR_ANIMATION)
            }
        }
    val animation =
        object {
            val progressMap = remember {
                mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
            }

            fun progress(nodeId: Int): Animatable<Float, AnimationVector1D>? =
                if (!hasFeature.APPEAR_ANIMATION) null
                else {
                    progressMap.computeIfAbsent(nodeId) {
                        Animatable(0f).also {
                            coroutineScope.launch {
                                it.animateTo(1f, tween(APPEAR_ANIMATION_DURATION_MS))
                            }
                        }
                    }
                }

            fun resetRemovedNodes(treeNodeStates: List<TreeNodeState>) {
                val nextSnapshotNodeIds = treeNodeStates.map { it.underlyingNodeId }
                progressMap
                    .filterKeys { it !in nextSnapshotNodeIds }
                    .forEach { (nodeId, _) -> progressMap.remove(nodeId) }
            }
        }
    val treeNodeList by
        treeNodeListFlow
            .onEach(animation::resetRemovedNodes)
            .collectAsStateWithLifecycle(emptyList())
    val treeState by treeStateFlow.collectAsStateWithLifecycle(TreeState())

    CompositionLocalProvider(LocalNodeDimensions provides createLocalNodeDimensions()) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(vertical = paddingHeight)
                    .verticalScrollShadows(shadowHeight)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            awaitFirstDown(false)
                            onDisableFlowStagger()
                        }
                    },
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(vertical = shadowHeight),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = treeNodeList.map { Pair(it, animation.progress(it.underlyingNodeId)) },
                    key = { it.first.key },
                    contentType = { it.first.underlyingNodeKind }
                ) { (initialTreeNodeState, appearAnimationProgress) ->
                    val treeNodeState by
                        initialTreeNodeState.flow.value.collectAsStateWithLifecycle(
                            initialTreeNodeState
                        )

                    NodeRow(
                        treeState = treeState,
                        treeNodeState = treeNodeState,
                        onSelectNode = { onSelectNode(treeNodeState.key) },
                        onActivatePayload = { onActivatePayload(treeNodeState) },
                        appearAnimationProgress = appearAnimationProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun paddingAndShadowHeight(): Pair<Dp, Dp> {
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
