package dev.fr33zing.launcher.ui.components.node.next

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

private const val APPEAR_ANIMATION_DURATION_MS = 350
private const val APPEAR_ANIMATION_TRANSLATE_DP = 25

@Composable
fun NodeTree(
    items: List<TreeNodeState>,
    features: NodeRowFeatureSet = NodeRowFeatures.All,
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
    val appearAnimation =
        object {
            val progressMap = remember {
                mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
            }

            fun progress(nodeId: Int): Animatable<Float, AnimationVector1D>? {
                if (!hasFeature.APPEAR_ANIMATION) return null

                if (nodeId !in progressMap)
                    progressMap[nodeId] =
                        Animatable(0f).also {
                            coroutineScope.launch {
                                it.animateTo(1f, tween(APPEAR_ANIMATION_DURATION_MS))
                            }
                        }
                return progressMap[nodeId]!!
            }
        }

    CompositionLocalProvider(LocalNodeDimensions provides createLocalNodeDimensions()) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(vertical = paddingHeight)
                    .verticalScrollShadows(shadowHeight),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(vertical = shadowHeight),
            ) {
                items(
                    items.map { Pair(it, appearAnimation.progress(it.nodePayload.node.nodeId)) },
                    key = { it.first.nodePayload.underlyingState.node.nodeId },
                    contentType = { it.first.nodePayload.node.kind }
                ) { (state, appearAnimationProgress) ->
                    LazyColumnItem(state, appearAnimationProgress)
                }
            }
        }
    }
}

@Composable
private fun LazyColumnItem(
    state: TreeNodeState,
    appearAnimationProgress: Animatable<Float, AnimationVector1D>?
) {
    CompositionLocalProvider(
        LocalNodeAppearance provides rememberNodeAppearance(state.nodePayload),
        LocalNodeRowFeatures provides NodeRowFeatures.All
    ) {
        NodeInteractions(state) {
            NodeRow(
                state,
                modifier =
                    Modifier.conditional(appearAnimationProgress != null) {
                        graphicsLayer {
                            translationY =
                                (1 - appearAnimationProgress!!.value) *
                                    APPEAR_ANIMATION_TRANSLATE_DP.dp.toPx()
                            alpha = appearAnimationProgress.value
                        }
                    }
            )
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