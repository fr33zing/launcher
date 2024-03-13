package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevanceWithHiddenChildren
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.interactive
import dev.fr33zing.launcher.ui.theme.foreground
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

val irrelevantNodeColor = foreground.dim(0.8f)

@Composable
fun NodeRow(
    treeState: TreeState? = null,
    treeNodeState: TreeNodeState,
    adjacentTreeNodeStates: AdjacentTreeNodeStates? = null,
    relevanceWithHiddenChildren: NodeRelevanceWithHiddenChildren? = null,
    nodeActions: NodeActions? = null,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onToggleNodeBatchSelected: () -> Unit = {},
    onMoveBatchSelectedNodes: (newParent: TreeNodeState) -> Unit = {},
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
                    val interactive = features.interactive()
                }
            }
        }

    @Composable
    fun Providers(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalNodeAppearance provides
                rememberNodeAppearance(
                    treeNodeState,
                    ignoreState =
                        !hasFeature.RENDER_STATE ||
                            (
                                !hasFeature.EXPAND_DIRECTORIES &&
                                    treeNodeState.value.node.kind == NodeKind.Directory
                            ),
                ),
            LocalNodeRowFeatures provides features,
            content = content,
        )

    @Composable
    fun Detail() {
        NodeDetailContainer(depth = treeNodeState.depth) {
            val label by
                remember(treeNodeState) {
                    derivedStateOf {
                        if (treeNodeState.value.isValidReference) {
                            treeNodeState.value.underlyingState.node.label
                        } else {
                            treeNodeState.value.node.label
                        }
                    }
                }
            NodeDetail(
                label,
                relevance = relevanceWithHiddenChildren?.relevance,
                isValidReference = treeNodeState.value.isValidReference,
                buildLabelString = buildLabelString,
                textModifier = textModifier(),
            )
            textEndContent?.invoke()
        }
    }

    @Composable
    fun Interactions(content: @Composable () -> Unit) =
        if (hasFeature.interactive) {
            NodeInteractions(
                treeState,
                treeNodeState,
                relevanceWithHiddenChildren?.relevance,
                adjacentTreeNodeStates,
                features,
                nodeActions,
                onSelectNode,
                onClearSelectedNode,
                onToggleNodeBatchSelected,
                onMoveBatchSelectedNodes,
                onActivatePayload,
                onActivateDirectory,
                onCreateNode,
                content = content,
            )
        } else {
            content()
        }

    @Composable
    fun Animation(content: @Composable () -> Unit) =
        if (hasFeature.APPEAR_ANIMATION && appearAnimationProgress != null) {
            NodeAppearAnimation(appearAnimationProgress, content)
        } else {
            content()
        }

    @Composable
    fun Relevance(content: @Composable () -> Unit) =
        if (relevanceWithHiddenChildren != null) {
            AnimatedVisibility(
                visible = relevanceWithHiddenChildren.relevance != NodeRelevance.Disruptive,
                enter = fadeIn() + expandVertically { -it },
                exit = fadeOut() + shrinkVertically { -it },
            ) {
                Column {
                    content()

                    // Hidden items indicator
                    AnimatedVisibility(
                        visible = relevanceWithHiddenChildren.hiddenChildren > 0,
                        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        NodeDetailContainer(depth = treeNodeState.depth + 1) {
                            val label =
                                remember(relevanceWithHiddenChildren) {
                                    buildString {
                                        append(relevanceWithHiddenChildren.hiddenChildren)
                                        append(" hidden item")
                                        append(if (relevanceWithHiddenChildren.hiddenChildren != 1) "s" else "")
                                    }
                                }
                            NodeDetail(label, color = irrelevantNodeColor, icon = Icons.Filled.VisibilityOff, lineThrough = false)
                        }
                    }
                }
            }
        } else {
            content()
        }

    Providers { Animation { Relevance { Interactions { Detail() } } } }
}
