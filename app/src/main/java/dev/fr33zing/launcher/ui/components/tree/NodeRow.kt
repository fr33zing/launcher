package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.interactive
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun NodeRow(
    simple: Boolean,
    treeState: TreeState? = null,
    treeNodeState: TreeNodeState,
    adjacentTreeNodeStates: AdjacentTreeNodeStates,
    nodeActions: NodeActions? = null,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onActivatePayload: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    appearAnimationProgress: Animatable<Float, AnimationVector1D>? = null,
) {
    val hasFeature by
        remember(features) {
            derivedStateOf {
                object {
                    val APPEAR_ANIMATION = features.contains(NodeRowFeatures.APPEAR_ANIMATION)
                    val interactive = features.interactive()
                }
            }
        }

    @Composable
    fun Providers(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalNodeAppearance provides rememberNodeAppearance(treeNodeState),
            LocalNodeRowFeatures provides features,
            content = content
        )

    @Composable
    fun Detail() {
        NodeDetailContainer(depth = treeNodeState.depth) {
            val label by
                remember(treeNodeState) {
                    derivedStateOf {
                        if (treeNodeState.value.isValidReference)
                            treeNodeState.value.underlyingState.node.label
                        else treeNodeState.value.node.label
                    }
                }
            NodeDetail(label, isValidReference = treeNodeState.value.isValidReference)
        }
    }

    @Composable
    fun Interactions(content: @Composable () -> Unit) =
        if (!simple && hasFeature.interactive && nodeActions != null)
            NodeInteractions(
                treeState,
                treeNodeState,
                adjacentTreeNodeStates,
                features,
                nodeActions,
                onSelectNode,
                onClearSelectedNode,
                onActivatePayload,
                onCreateNode,
                content = content
            )
        else content()

    @Composable
    fun Animation(content: @Composable () -> Unit) =
        if (hasFeature.APPEAR_ANIMATION && appearAnimationProgress != null)
            NodeAppearAnimation(appearAnimationProgress, content)
        else content()

    Providers { Animation { Interactions { Detail() } } }
}
