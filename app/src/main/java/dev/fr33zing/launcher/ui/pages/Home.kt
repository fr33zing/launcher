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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.utility.detectFlingUp
import dev.fr33zing.launcher.ui.components.Clock
import dev.fr33zing.launcher.ui.components.node.NodeIconAndText
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

private val horizontalPadding = 16.dp

@Composable
fun Home(db: AppDatabase, navController: NavController) {
    fun onFlingUp() {
        navController.navigate("home/tree/$ROOT_NODE_ID")
    }

    BackHandler {
        // Prevent back button loop
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier =
            Modifier.systemBarsPadding().padding(vertical = 32.dp).fillMaxSize().pointerInput(
                Unit
            ) {
                detectFlingUp(::onFlingUp)
            },
    ) {
        Clock(horizontalPadding)
        HomeNodeList(db, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HomeNodeList(db: AppDatabase, modifier: Modifier = Modifier) {
    val homeNodes = remember { mutableStateListOf<Node>() }

    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current

    val fontSize by preferences.getFontSize()
    val spacing by preferences.getSpacing()
    val lineHeight = with(localDensity) { fontSize.toDp() }

    LaunchedEffect(Unit) {
        val homeNode = db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home)
        homeNodes.addAll(db.nodeDao().getChildNodes(homeNode.nodeId).sortedBy { it.order })
    }

    Column(verticalArrangement = Arrangement.Center, modifier = modifier) {
        homeNodes.forEach { HomeNode(db, it, fontSize, spacing, lineHeight) }
    }
}

@Composable
private fun HomeNode(db: AppDatabase, node: Node, fontSize: TextUnit, spacing: Dp, lineHeight: Dp) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf<Payload?>(null) }

    LaunchedEffect(node) { payload = db.getPayloadByNodeId(node.kind, node.nodeId) }

    if (payload != null) {
        val color = node.kind.color(payload)
        val icon = node.kind.icon(payload)
        val text = node.label
        val lineThrough = node.kind.lineThrough(payload)

        val interactionSource = remember(node) { MutableInteractionSource() }
        val indication = rememberCustomIndication(color = color)

        Box(
            Modifier.clickable(
                interactionSource,
                indication,
                onClick = { payload!!.activate(db, context) }
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = spacing / 2)
            ) {
                NodeIconAndText(
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    label = text,
                    color = color,
                    icon = icon,
                    lineThrough = lineThrough,
                )
            }
        }
    }
}
