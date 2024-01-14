package dev.fr33zing.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.fr33zing.launcher.data.viewmodel.utility.TreeBrowserStateHolder
import dev.fr33zing.launcher.ui.components.node.NodeIconAndText
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun TreeBrowser(
    stateHolder: TreeBrowserStateHolder,
    onNodeSelected: (NodePayloadState) -> Unit,
    additionalRowContent: @Composable (NodePayloadState) -> Unit = {}
) {
    val treeBrowserState by stateHolder.flow.collectAsStateWithLifecycle(null)
    if (treeBrowserState == null) return

    val preferences = Preferences(LocalContext.current)
    val fontSize by preferences.nodeAppearance.fontSize.state
    val spacing by preferences.nodeAppearance.spacing.state
    val lineHeight = with(LocalDensity.current) { fontSize.toDp() }

    AnimatedContent(
        targetState = treeBrowserState!!,
        label = "tree browser",
        transitionSpec = {
            val animationDirection = treeBrowserState!!.direction.depthChange
            val animationDuration = 600
            (fadeIn(tween(animationDuration)) +
                slideInHorizontally(tween(animationDuration)) {
                    it * animationDirection
                }) togetherWith
                (fadeOut(tween(animationDuration)) +
                    slideOutHorizontally(tween(animationDuration)) { -it * animationDirection })
        },
    ) { state ->
        val children by state.children.flow.collectAsStateWithLifecycle(emptyArray())
        LazyColumn {
            items(children, key = { it.node.nodeId }) { nodePayload ->
                TreeBrowserRow(
                    nodePayload = nodePayload,
                    fontSize = fontSize,
                    spacing = spacing,
                    lineHeight = lineHeight,
                    onNodeSelected = { onNodeSelected(nodePayload) },
                ) {
                    additionalRowContent(nodePayload)
                }
            }
        }
    }
}

@Composable
private fun TreeBrowserRow(
    nodePayload: NodePayloadState,
    fontSize: TextUnit,
    spacing: Dp,
    lineHeight: Dp,
    onNodeSelected: () -> Unit,
    additionalContent: @Composable () -> Unit,
) {
    val (node) = nodePayload
    val nodeAppearance = rememberNodeAppearance(nodePayload)

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication()

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
                label = node.label,
                color = nodeAppearance.color,
                icon = nodeAppearance.icon,
                textModifier = Modifier.weight(1f),
            )
        }

        additionalContent()
    }
}
