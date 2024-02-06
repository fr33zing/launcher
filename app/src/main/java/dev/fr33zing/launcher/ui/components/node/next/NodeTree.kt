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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.node.next.utility.LocalNodeRowFeatures
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.node.next.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.node.next.utility.createLocalNodeDimensions
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val APPEAR_ANIMATION_DURATION_MS = 350
private const val APPEAR_ANIMATION_STAGGER_MS: Long = 15

@Composable
fun NodeTree(
    flow: Flow<List<TreeNodeState>>,
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    disableFlowStagger: () -> Unit = {},
    activate: (TreeNodeState) -> Unit = {},
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
            val staggerMutex = Mutex()
            val progressMap = remember {
                mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
            }

            fun progress(nodeId: Int): Animatable<Float, AnimationVector1D>? =
                if (!hasFeature.APPEAR_ANIMATION) null
                else {
                    progressMap.computeIfAbsent(nodeId) {
                        Animatable(0f).also {
                            coroutineScope.launch {
                                staggerMutex.withLock { delay(APPEAR_ANIMATION_STAGGER_MS) }
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
    val state by flow.onEach(animation::resetRemovedNodes).collectAsStateWithLifecycle(emptyList())

    CompositionLocalProvider(LocalNodeDimensions provides createLocalNodeDimensions()) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(vertical = paddingHeight)
                    .verticalScrollShadows(shadowHeight)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            awaitFirstDown(false)
                            disableFlowStagger()
                        }
                    },
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(vertical = shadowHeight),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.map { Pair(it, animation.progress(it.underlyingNodeId)) },
                    key = { it.first.key },
                    contentType = { it.first.underlyingNodeKind }
                ) { (state, progress) ->
                    val liveState by state.flow.value.collectAsStateWithLifecycle(null)

                    LazyColumnItem(liveState ?: state, progress) { activate(state) }
                }
            }
        }
    }
}

@Composable
private fun LazyColumnItem(
    state: TreeNodeState,
    progress: Animatable<Float, AnimationVector1D>?,
    activate: () -> Unit
) {
    val dimensions = LocalNodeDimensions.current

    Box(
        Modifier.conditional(progress != null) {
            graphicsLayer {
                translationY = (1 - progress!!.value) * (dimensions.lineHeight.toPx() * 0.75f)
                alpha = progress.value
            }
        }
    ) {
        CompositionLocalProvider(
            LocalNodeAppearance provides rememberNodeAppearance(state),
            LocalNodeRowFeatures provides NodeRowFeatures.All
        ) {
            NodeInteractions(state, activate) { NodeRow(state) }
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
