package dev.fr33zing.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadWithReferenceTargetState
import dev.fr33zing.launcher.data.viewmodel.utility.TreeBrowserStateHolder
import dev.fr33zing.launcher.ui.components.node.NodeIconAndText
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun TreeBrowser(
    stateHolder: TreeBrowserStateHolder,
    modifier: Modifier = Modifier,
    additionalRowContent: @Composable (NodePayloadState) -> Unit = {},
) {
    val state by stateHolder.flow.collectAsStateWithLifecycle(null)

    val preferences = Preferences(LocalContext.current)
    val fontSize by preferences.nodeAppearance.fontSize.state
    val spacing by preferences.nodeAppearance.spacing.state
    val lineHeight = with(LocalDensity.current) { fontSize.toDp() }

    if (state == null) return

    BackHandler(enabled = state!!.canTraverseUpward) { stateHolder.traverseUpward(state!!) }

    AnimatedContent(
        targetState = state!!,
        label = "tree browser",
        transitionSpec = {
            val animationDirection = state!!.direction.depthChange
            val animationDuration = 600
            (fadeIn(tween(animationDuration)) +
                slideInHorizontally(tween(animationDuration)) {
                    it * animationDirection
                }) togetherWith
                (fadeOut(tween(animationDuration)) +
                    slideOutHorizontally(tween(animationDuration)) { -it * animationDirection })
        },
        modifier = modifier,
    ) { targetState ->
        val children by
            targetState.children.flowWithReferenceTargetState.collectAsStateWithLifecycle(
                emptyArray()
            )
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            if (targetState.canTraverseUpward)
                TraverseUpRow(fontSize, spacing, lineHeight) {
                    stateHolder.traverseUpward(currentState = targetState)
                }

            for (nodePayload in children) {
                key(nodePayload.node.nodeId) {
                    TreeBrowserRow(
                        state = nodePayload,
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
    fontSize: TextUnit,
    spacing: Dp,
    lineHeight: Dp,
    onTraverseUp: () -> Unit,
) {
    val label = remember { ".." }
    val color = remember { Foreground.mix(Background, 0.5f) }
    val icon = remember { Icons.Outlined.DriveFolderUpload }

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color)

    Row(
        modifier =
            Modifier.clickable(interactionSource, indication, onClick = onTraverseUp)
                .padding(horizontal = ScreenHorizontalPadding, vertical = spacing / 2)
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
    state: NodePayloadWithReferenceTargetState,
    fontSize: TextUnit,
    spacing: Dp,
    lineHeight: Dp,
    onNodeSelected: () -> Unit,
    additionalContent: @Composable () -> Unit,
) {
    val nodeAppearance = rememberNodeAppearance(state)

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(nodeAppearance.color)

    Row(
        modifier =
            Modifier.clickable(interactionSource, indication, onClick = onNodeSelected)
                .padding(horizontal = ScreenHorizontalPadding, vertical = spacing / 2)
                .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = state.node.label,
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
