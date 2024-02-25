package dev.fr33zing.launcher.ui.components.tree

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SubdirectoryArrowLeft
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.viewmodel.state.ReferenceFollowingNodePayloadState
import dev.fr33zing.launcher.data.viewmodel.state.TreeBrowserStateHolder
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.dim
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

// TODO handle Y overflow. Needs to have two modes, truncate and scroll.

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TreeBrowser(
    stateHolder: TreeBrowserStateHolder,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ScreenHorizontalPadding,
    center: Boolean = false,
    additionalRowContent: @Composable (ReferenceFollowingNodePayloadState) -> Unit = {},
) {
    val state by stateHolder.flow.collectAsStateWithLifecycle(null)

    if (state == null) return

    BackHandler(enabled = state!!.canTraverseUpward) { stateHolder.traverseUpward() }

    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        AnimatedContent(
            targetState = state!!,
            label = "tree browser",
            transitionSpec = {
                val direction = state!!.direction.depthChange
                fadeIn() + slideInHorizontally { it * direction } togetherWith
                    fadeOut() + slideOutHorizontally { -it * direction }
            },
            modifier = modifier,
        ) { targetState ->
            val children by targetState.children.flow.collectAsStateWithLifecycle(emptyArray())
            Column(
                verticalArrangement = if (center) Arrangement.Center else Arrangement.Top,
                modifier = if (center) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
            ) {
                if (targetState.canTraverseUpward) {
                    TraverseUpRow(
                        enabled = !transition.isRunning,
                        horizontalPadding = horizontalPadding,
                        onTraverseUp = stateHolder::traverseUpward
                    )
                }

                for (nodePayload in children) {
                    key(nodePayload.node.nodeId) {
                        TreeBrowserRow(
                            enabled = !transition.isRunning,
                            state = nodePayload,
                            horizontalPadding = horizontalPadding,
                            onNodeSelected = { stateHolder.selectNode(nodePayload) },
                        ) {
                            additionalRowContent(nodePayload)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TraverseUpRow(
    enabled: Boolean,
    horizontalPadding: Dp,
    onTraverseUp: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val label = remember { ".." }
    val color = remember { Foreground.dim() }
    val icon =
        remember(layoutDirection) {
            if (layoutDirection == LayoutDirection.Ltr) Icons.Outlined.SubdirectoryArrowLeft
            else Icons.Outlined.SubdirectoryArrowRight
        }

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color)

    NodeDetailContainer(
        Modifier.clickable(interactionSource, indication) { if (enabled) onTraverseUp() }
    ) {
        NodeDetail(
            label = label,
            color = color,
            icon = icon,
            textModifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TreeBrowserRow(
    enabled: Boolean,
    state: ReferenceFollowingNodePayloadState,
    horizontalPadding: Dp,
    onNodeSelected: () -> Unit,
    additionalContent: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalNodeAppearance provides
            rememberNodeAppearance(
                nodePayload = state,
                ignoreState = state.node.kind == NodeKind.Directory,
            )
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val indication = rememberCustomIndication(LocalNodeAppearance.current.color)

        NodeDetailContainer(
            Modifier.clickable(interactionSource, indication) { if (enabled) onNodeSelected() }
        ) {
            NodeDetail(
                label = state.underlyingState.node.label,
                isValidReference = state.isValidReference,
                textModifier = Modifier.weight(1f)
            )

            additionalContent()
        }
    }
}
