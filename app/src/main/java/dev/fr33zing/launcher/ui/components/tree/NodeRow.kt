package dev.fr33zing.launcher.ui.components.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatureSet
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.interactive
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun NodeRow(
    treeNodeState: TreeNodeState,
    treeState: TreeState? = null,
    nodeActions: NodeActions? = null,
    onSelectNode: () -> Unit = {},
    onClearSelectedNode: () -> Unit = {},
    onActivatePayload: () -> Unit = {},
    features: NodeRowFeatureSet = NodeRowFeatures.All,
    appearAnimationProgress: Animatable<Float, AnimationVector1D>? = null,
) {
    val (node) = treeNodeState.value
    val dimensions = LocalNodeDimensions.current
    val indent by remember { derivedStateOf { dimensions.indent * treeNodeState.depth } }
    val hasFeature =
        remember(features) {
            object {
                val APPEAR_ANIMATION = features.contains(NodeRowFeatures.APPEAR_ANIMATION)
                val interactive = features.interactive()
            }
        }
    val layers =
        remember(treeNodeState, treeState, features) {
            object {
                @Composable
                fun Providers(content: @Composable () -> Unit) =
                    CompositionLocalProvider(
                        LocalNodeAppearance provides rememberNodeAppearance(treeNodeState),
                        LocalNodeRowFeatures provides features,
                        content = content
                    )

                @Composable
                fun Container(content: @Composable RowScope.() -> Unit) =
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(
                                    vertical = dimensions.spacing / 2,
                                    horizontal = ScreenHorizontalPadding,
                                )
                                .padding(start = indent),
                        content = content
                    )

                @Composable
                fun Detail() =
                    NodeDetail(
                        label =
                            if (treeNodeState.value.isValidReference)
                                treeNodeState.value.underlyingState.node.label
                            else node.label,
                        isValidReference = treeNodeState.value.isValidReference
                    )

                val features =
                    object {
                        @Composable
                        fun Interactions(content: @Composable () -> Unit) =
                            if (hasFeature.interactive && nodeActions != null)
                                NodeInteractions(
                                    treeState,
                                    treeNodeState,
                                    features,
                                    nodeActions,
                                    onSelectNode,
                                    onClearSelectedNode,
                                    onActivatePayload,
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
        layers.features.Animation {
            layers.features.Interactions { layers.Container { layers.Detail() } }
        }
    }
}
