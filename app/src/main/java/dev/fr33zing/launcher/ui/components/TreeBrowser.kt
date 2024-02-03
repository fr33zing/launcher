package dev.fr33zing.launcher.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SubdirectoryArrowLeft
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadWithReferenceTargetState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeBrowserStateHolder
import dev.fr33zing.launcher.ui.components.node.NodeIconAndText
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
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
    additionalRowContent: @Composable (NodePayloadState) -> Unit = {},
) {
    val state by stateHolder.flow.collectAsStateWithLifecycle(null)

    val preferences = Preferences(LocalContext.current)
    val fontSize by preferences.nodeAppearance.fontSize.state
    val spacing by preferences.nodeAppearance.spacing.state
    val lineHeight = with(LocalDensity.current) { fontSize.toDp() }

    if (state == null) return

    BackHandler(enabled = state!!.canTraverseUpward) { stateHolder.traverseUpward() }

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
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            if (targetState.canTraverseUpward) {
                TraverseUpRow(
                    enabled = !transition.isRunning,
                    horizontalPadding = horizontalPadding,
                    fontSize = fontSize,
                    spacing = spacing,
                    lineHeight = lineHeight,
                    onTraverseUp = stateHolder::traverseUpward
                )
            }

            for (nodePayload in children) {
                key(nodePayload.node.nodeId) {
                    TreeBrowserRow(
                        enabled = !transition.isRunning,
                        state = nodePayload,
                        horizontalPadding = horizontalPadding,
                        fontSize = fontSize,
                        spacing = spacing,
                        lineHeight = lineHeight,
                        onNodeSelected = { stateHolder.selectNode(nodePayload) },
                    ) {
                        additionalRowContent(nodePayload)
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
    fontSize: TextUnit,
    spacing: Dp,
    lineHeight: Dp,
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

    Row(
        modifier =
            Modifier.clickable(interactionSource, indication) { if (enabled) onTraverseUp() }
                .padding(horizontal = horizontalPadding, vertical = spacing / 2)
                .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = label,
                color = color,
                icon = icon,
                textModifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TreeBrowserRow(
    enabled: Boolean,
    state: NodePayloadWithReferenceTargetState,
    horizontalPadding: Dp,
    fontSize: TextUnit,
    spacing: Dp,
    lineHeight: Dp,
    onNodeSelected: () -> Unit,
    additionalContent: @Composable () -> Unit,
) {
    val (node) = state
    val nodeAppearance =
        rememberNodeAppearance(
            nodePayload = state,
            ignoreState = node.kind == NodeKind.Directory,
        )

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(nodeAppearance.color)

    Row(
        modifier =
            Modifier.clickable(interactionSource, indication) { if (enabled) onNodeSelected() }
                .padding(horizontal = horizontalPadding, vertical = spacing / 2)
                .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = node.label,
                color = nodeAppearance.color,
                icon = nodeAppearance.icon,
                lineThrough = nodeAppearance.lineThrough,
                isValidReference = state.isValidReference,
                textModifier = Modifier.weight(1f),
            )
        }

        additionalContent()
    }
}
