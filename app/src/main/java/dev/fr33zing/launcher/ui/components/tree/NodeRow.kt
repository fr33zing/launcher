package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.hasPermission
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
    val hasFeature =
        remember(features) {
            object {
                val APPEAR_ANIMATION = features.contains(NodeRowFeatures.APPEAR_ANIMATION)
                val interactive = features.interactive()
            }
        }
    val layers =
        remember(treeState, treeNodeState) {
            object {
                @Composable
                fun Providers(content: @Composable () -> Unit) =
                    CompositionLocalProvider(
                        LocalNodeAppearance provides rememberNodeAppearance(treeNodeState),
                        LocalNodeRowFeatures provides features,
                        content = content
                    )

                @Composable
                fun Detail() =
                    NodeDetailContainer(treeNodeState.depth) {
                        val self =
                            treeNodeState.permissions.hasPermission(
                                PermissionKind.Create,
                                PermissionScope.Self
                            )
                        val children =
                            treeNodeState.permissions.hasPermission(
                                PermissionKind.Create,
                                PermissionScope.Recursive
                            )
                        Text("  ${if (self) "x" else "-"}${if (children) "x" else "-"}")
                        NodeDetail(
                            label =
                                if (treeNodeState.value.isValidReference)
                                    treeNodeState.value.underlyingState.node.label
                                else treeNodeState.value.node.label,
                            isValidReference = treeNodeState.value.isValidReference
                        )
                    }

                val features =
                    object {
                        @Composable
                        fun Interactions(content: @Composable () -> Unit) =
                            if (hasFeature.interactive && nodeActions != null)
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
                    }
            }
        }

    layers.Providers {
        layers.features.Animation { layers.features.Interactions { layers.Detail() } }
    }
}
