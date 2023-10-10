package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.nodeIndent
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.helper.verticalScrollShadows
import dev.fr33zing.launcher.ui.components.NodeIconAndText
import dev.fr33zing.launcher.ui.components.NodePath
import dev.fr33zing.launcher.ui.components.OutlinedReadOnlyValue
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix

private val extraPadding = 6.dp
private val verticalPadding = Preferences.spacingDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Move(db: AppDatabase, navController: NavController, nodeId: Int) {
    var node by remember { mutableStateOf<Node?>(null) }

    LaunchedEffect(Unit) {
        node = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Node is null")
    }

    if (node == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            append("Moving ")
                            withStyle(SpanStyle(color = node!!.kind.color)) {
                                append(node!!.kind.label)
                            }
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            //                        cancelDialogVisible.value = true
                        }
                    ) {
                        Icon(Icons.Filled.Close, "cancel", tint = Catppuccin.Current.red)
                    }
                    IconButton(
                        onClick = {
                            //                        saveDialogVisible.value = true
                        }
                    ) {
                        Icon(Icons.Filled.Check, "finish", tint = Catppuccin.Current.green)
                    }
                },
            )
        },
    ) { innerPadding ->
        val subNavController = rememberNavController()
        val startDestination = "view/${node!!.parentId ?: ROOT_NODE_ID}"

        NavHost(
            subNavController,
            startDestination = "start",
            enterTransition = { slideInHorizontally() + fadeIn() },
            exitTransition = { slideOutHorizontally() + fadeOut() },
            modifier = Modifier.fillMaxSize()
        ) {
            composable("start") {
                LaunchedEffect(Unit) { subNavController.navigate(startDestination) }
            }
            composable("view/{rootNodeId}") { backStackEntry ->
                val rootNodeId = backStackEntry.arguments?.getString("rootNodeId")!!.toInt()
                var rootNode by remember { mutableStateOf<Node?>(null) }

                LaunchedEffect(Unit) {
                    rootNode =
                        db.nodeDao().getNodeById(rootNodeId) ?: throw Exception("Node is null")
                }

                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = extraPadding)
                ) {
                    if (rootNode != null)
                        DirectoryPicker(db, navController, subNavController, node!!, rootNode!!)
                }
            }
        }
    }
}

@Composable
private fun DirectoryPicker(
    db: AppDatabase,
    navController: NavController,
    subNavController: NavHostController,
    node: Node,
    rootNode: Node,
) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val spacing = Preferences.spacingDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }
    val indent = remember { nodeIndent(1, Preferences.indentDefault, lineHeight) }

    val nodes = remember { mutableStateListOf<Node>() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        nodes.addAll(
            db.nodeDao().getChildNodes(rootNode.nodeId).filter { it.kind == NodeKind.Directory }
        )
    }

    BackHandler { subNavController.navigate("view/${rootNode.parentId ?: ROOT_NODE_ID}") }

    Column(
        verticalArrangement = Arrangement.spacedBy(Preferences.spacingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(
            "Go back to navigate up or tap a directory in the destination box below.",
            color = Foreground.mix(Background, 0.5f),
            fontStyle = FontStyle.Italic,
        )

        OutlinedReadOnlyValue(label = "Destination", modifier = Modifier.fillMaxWidth()) {
            NodePath(db, rootNode)
        }

        if (nodes.isEmpty()) {
            Text(
                "This directory is empty.",
                color = Foreground.mix(Background, 0.5f),
                fontStyle = FontStyle.Italic,
            )
        } else {
            Box(Modifier.verticalScrollShadows(Preferences.spacingDefault / 2)) {
                Column(
                    Modifier.verticalScroll(scrollState)
                        .padding(vertical = Preferences.spacingDefault / 2)
                ) {
                    nodes.forEach { node ->
                        var payload by remember { mutableStateOf<Payload?>(null) }
                        LaunchedEffect(node) {
                            payload = db.getPayloadByNodeId(node.kind, node.nodeId)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.padding(vertical = Preferences.spacingDefault / 2)
                                    .fillMaxWidth()
                                    .clickable { subNavController.navigate("view/${node.nodeId}") }
                        ) {
                            NodeIconAndText(
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                label = node.label,
                                color = node.kind.color(payload, ignoreState = true),
                                icon = node.kind.icon(payload, ignoreState = true),
                            )
                        }
                    }
                }
            }
        }
    }
}
