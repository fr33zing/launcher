package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
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
    simple: Boolean = false,
    treeState: TreeState? = null,
    treeNodeState: TreeNodeState,
    adjacentTreeNodeStates: AdjacentTreeNodeStates? = null,
    nodeActions: NodeActions? = null,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onToggleNodeMultiSelected: () -> Unit = {},
    onActivatePayload: () -> Unit = {},
    /** Only called when [NodeRowFeatures.RECURSIVE] is not present. */
    onActivateDirectory: () -> Unit = {},
    onCreateNode: (RelativeNodePosition, NodeKind) -> Unit = { _, _ -> },
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    appearAnimationProgress: Animatable<Float, AnimationVector1D>? = null,
    buildLabelString: (AnnotatedString.Builder.() -> Unit)? = null,
    textModifier: RowScope.() -> Modifier = { Modifier },
    textEndContent: (@Composable () -> Unit)? = null,
) {
    val hasFeature by
        remember(features) {
            derivedStateOf {
                object {
                    val RENDER_STATE = features.contains(NodeRowFeatures.RENDER_STATE)
                    val EXPAND_DIRECTORIES = features.contains(NodeRowFeatures.RECURSIVE)
                    val APPEAR_ANIMATION = features.contains(NodeRowFeatures.APPEAR_ANIMATION)
                    val MODAL = features.contains(NodeRowFeatures.MODAL)
                    val interactive = features.interactive()
                }
            }
        }
    val relevance =
        remember(treeState, treeNodeState, features) {
            if (!hasFeature.MODAL) null
            else
                treeState?.mode?.relevance?.invoke(treeState, treeNodeState)
                    ?: NodeRelevance.Relevant
        }

    @Composable
    fun Providers(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalNodeAppearance provides
                rememberNodeAppearance(
                    treeNodeState,
                    ignoreState =
                        !hasFeature.RENDER_STATE ||
                            (!hasFeature.EXPAND_DIRECTORIES &&
                                treeNodeState.value.node.kind == NodeKind.Directory)
                ),
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
            NodeDetail(
                label,
                relevance = relevance,
                isValidReference = treeNodeState.value.isValidReference,
                buildLabelString = buildLabelString,
                textModifier = textModifier(),
            )
            textEndContent?.invoke()
        }
    }

    @Composable
    fun Interactions(content: @Composable () -> Unit) =
        if (!simple && hasFeature.interactive)
            NodeInteractions(
                treeState,
                treeNodeState,
                relevance,
                adjacentTreeNodeStates,
                features,
                nodeActions,
                onSelectNode,
                onClearSelectedNode,
                onToggleNodeMultiSelected,
                onActivatePayload,
                onActivateDirectory,
                onCreateNode,
                content = content
            )
        else content()

    @Composable
    fun Animation(content: @Composable () -> Unit) =
        if (hasFeature.APPEAR_ANIMATION && appearAnimationProgress != null)
            NodeAppearAnimation(appearAnimationProgress, content)
        else content()

    @Composable
    fun Relevance(content: @Composable () -> Unit) =
        if (relevance != null)
            AnimatedVisibility(
                visible = relevance != NodeRelevance.Disruptive,
                enter = fadeIn() + expandVertically { -it },
                exit = fadeOut() + shrinkVertically { -it },
            ) {
                content()
            }
        else content()

    Providers { Animation { Relevance { Interactions { Detail() } } } }
}
