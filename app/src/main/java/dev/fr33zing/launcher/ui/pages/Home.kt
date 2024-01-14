package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.viewmodel.HomeViewModel
import dev.fr33zing.launcher.ui.components.Clock
import dev.fr33zing.launcher.ui.components.TreeBrowser
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.detectFlingUp
import dev.fr33zing.launcher.ui.utility.longPressable

@Composable
fun Home(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    //    val nodePayloads by viewModel.nodePayloads.collectAsStateWithLifecycle()

    val context = LocalContext.current

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
        Column(verticalArrangement = Arrangement.SpaceAround, modifier = Modifier.weight(1f)) {
            TreeBrowser(viewModel.treeBrowser, onNodeSelected = { it.activate(context) })
        }
    }
}

// @Composable
// private fun HomeNodeList(nodePayloads: Array<NodePayloadState>, modifier: Modifier = Modifier) {
//    val preferences = Preferences(LocalContext.current)
//    val localDensity = LocalDensity.current
//
//    val fontSize by preferences.nodeAppearance.fontSize.state
//    val spacing by preferences.nodeAppearance.spacing.state
//    val lineHeight = remember(fontSize, localDensity) { with(localDensity) { fontSize.toDp() } }
//
//    Column(verticalArrangement = Arrangement.Center, modifier = modifier) {
//        nodePayloads.forEach { nodePayload ->
//            key(nodePayload.node.nodeId) {
//                HomeNode(
//                    nodePayload,
//                    fontSize,
//                    spacing,
//                    lineHeight,
//                )
//            }
//        }
//    }
// }

// @Composable
// private fun HomeNode(
//    nodePayload: NodePayloadState,
//    fontSize: TextUnit,
//    spacing: Dp,
//    lineHeight: Dp,
// ) {
//    val context = LocalContext.current
//    val (node) = nodePayload
//    val (color, icon, lineThrough) = rememberNodeAppearance(nodePayload)
//
//    val interactionSource = remember(node) { MutableInteractionSource() }
//    val indication = rememberCustomIndication(color = color)
//
//    Box(
//        Modifier.clickable(
//            interactionSource,
//            indication,
//            onClick = { nodePayload.activate(context) }
//        )
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier =
//                Modifier.fillMaxWidth()
//                    .padding(horizontal = ScreenHorizontalPadding, vertical = spacing / 2)
//        ) {
//            NodeIconAndText(
//                fontSize = fontSize,
//                lineHeight = lineHeight,
//                label = node.label,
//                color = color,
//                icon = icon,
//                lineThrough = lineThrough,
//            )
//        }
//    }
// }
