package dev.fr33zing.launcher.ui.components.tree.utility

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.components.tree.APPEAR_ANIMATION_DURATION_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Immutable
class NodeTreeAppearAnimations(
    private val features: NodeRowFeatureSet,
    private val coroutineScope: CoroutineScope,
    treeNodeListFlow: StateFlow<List<TreeNodeState>>,
) {
    val flow =
        treeNodeListFlow.onEach(::resetRemovedNodes)
            .map { treeNodeList -> treeNodeList.map { Pair(it, progress(it.key)) } }

    private val progressMap =
        mutableStateMapOf<TreeNodeKey, Animatable<Float, AnimationVector1D>>()

    private fun progress(key: TreeNodeKey): Animatable<Float, AnimationVector1D>? =
        if (!features.contains(NodeRowFeatures.APPEAR_ANIMATION)) {
            null
        } else {
            progressMap.computeIfAbsent(key) {
                Animatable(0f).also {
                    coroutineScope.launch { it.animateTo(1f, tween(APPEAR_ANIMATION_DURATION_MS)) }
                }
            }
        }

    private fun resetRemovedNodes(treeNodeStates: List<TreeNodeState>) {
        val nextSnapshotKeys = treeNodeStates.map { it.key }
        progressMap
            .filterKeys { key -> key !in nextSnapshotKeys }
            .forEach { (nodeId, _) -> progressMap.remove(nodeId) }
    }
}
