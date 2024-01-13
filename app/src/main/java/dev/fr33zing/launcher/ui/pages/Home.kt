package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.viewmodel.HomeViewModel
import dev.fr33zing.launcher.data.viewmodel.utility.NodePayloadState
import dev.fr33zing.launcher.ui.components.Clock
import dev.fr33zing.launcher.ui.components.node.NodeIconAndText
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.detectFlingUp
import dev.fr33zing.launcher.ui.utility.longPressable
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance

@Composable
fun Home(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val nodePayloads by viewModel.nodePayloads.collectAsStateWithLifecycle()

    fun onFlingUp() = navController.navigate("home/tree/$ROOT_NODE_ID")

    BackHandler { /* Prevent back button loop */}

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier =
            Modifier.systemBarsPadding()
                .padding(vertical = 32.dp)
                .fillMaxSize()
                .pointerInput(Unit) { detectFlingUp(::onFlingUp) }
                .longPressable { navController.navigate("settings") },
    ) {
        Clock(ScreenHorizontalPadding)
        HomeNodeList(nodePayloads, Modifier.weight(1f))
    }
}

@Composable
private fun HomeNodeList(nodePayloads: Array<NodePayloadState>, modifier: Modifier = Modifier) {
    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current

    val fontSize by preferences.nodeAppearance.fontSize.state
    val spacing by preferences.nodeAppearance.spacing.state
    val lineHeight = remember(fontSize, localDensity) { with(localDensity) { fontSize.toDp() } }

    Column(verticalArrangement = Arrangement.Center, modifier = modifier) {
        nodePayloads.forEach { nodePayload ->
            key(nodePayload.node.nodeId) {
                HomeNode(
                    nodePayload,
                    fontSize,
                    spacing,
                    lineHeight,
                )
            }
        }
    }
}

@Composable
private fun HomeNode(
    nodePayload: NodePayloadState,
    fontSize: TextUnit,
    spacing: Dp,
    lineHeight: Dp,
) {
    val context = LocalContext.current
    val (node) = nodePayload
    val (color, icon, lineThrough) = rememberNodeAppearance(nodePayload)

    val interactionSource = remember(node) { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color)

    Box(
        Modifier.clickable(
            interactionSource,
            indication,
            onClick = { nodePayload.activate(context) }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = spacing / 2)
        ) {
            NodeIconAndText(
                fontSize = fontSize,
                lineHeight = lineHeight,
                label = node.label,
                color = color,
                icon = icon,
                lineThrough = lineThrough,
            )
        }
    }
}
