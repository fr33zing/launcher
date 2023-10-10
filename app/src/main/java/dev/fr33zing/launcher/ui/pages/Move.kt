package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.moveNode
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.helper.verticalScrollShadows
import dev.fr33zing.launcher.ui.components.NodeIconAndText
import dev.fr33zing.launcher.ui.components.NodePath
import dev.fr33zing.launcher.ui.components.OutlinedReadOnlyValue
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val extraPadding = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Move(db: AppDatabase, navController: NavController, nodeId: Int) {
    var node by remember { mutableStateOf<Node?>(null) }
    var rootNodeId by remember { mutableStateOf<Int?>(null) }
    var rootNode by remember { mutableStateOf<Node?>(null) }
    var dirsAndParent by remember { mutableStateOf<Pair<List<Node>, Node?>>(Pair(listOf(), null)) }
    var animationDirection by remember { mutableIntStateOf(-1) }
    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    fun setRootNode(nodeId: Int?, animDirection: Int) {
        animationDirection = animDirection

        CoroutineScope(Dispatchers.IO).launch {
            rootNodeId = nodeId
            rootNode =
                db.nodeDao().getNodeById(rootNodeId ?: ROOT_NODE_ID)
                    ?: throw Exception("Root node is null")
            val parentNode = rootNode!!.parentId?.let { db.nodeDao().getNodeById(it) }
            val dirs =
                db.nodeDao().getChildNodes(rootNode!!.nodeId).filter {
                    it.kind == NodeKind.Directory
                }
            dirsAndParent =
                Pair(
                    if (parentNode != null) listOf(parentNode) + dirs else dirs,
                    parentNode,
                )
        }
    }

    LaunchedEffect(nodeId) {
        node = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Node is null")
        rootNodeId = node!!.parentId
        setRootNode(rootNodeId, 0)
    }

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel move",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue browsing",
        noIcon = Icons.Filled.ArrowBack,
        backAction = YesNoDialogBackAction.Yes,
        onYes = { onCancelChanges(navController) },
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Confirm move",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue browsing",
        noIcon = Icons.Filled.ArrowBack,
        onYes = { onSaveChanges(navController, db, node!!, rootNodeId) },
    )

    BackHandler(enabled = rootNode?.parentId == null) { cancelDialogVisible.value = true }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            append("Moving ")
                            if (node != null) {
                                withStyle(SpanStyle(color = node!!.kind.color)) {
                                    append(node!!.kind.label)
                                }
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { cancelDialogVisible.value = true }) {
                        Icon(Icons.Filled.Close, "cancel", tint = Catppuccin.Current.red)
                    }
                    IconButton(onClick = { saveDialogVisible.value = true }) {
                        Icon(Icons.Filled.Check, "finish", tint = Catppuccin.Current.green)
                    }
                },
            )
        },
    ) { innerPadding ->
        if (node != null && rootNode != null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(extraPadding)) {
                DirectoryPicker(
                    db,
                    animationDirection,
                    dirsAndParent,
                    node!!,
                    rootNode!!,
                    ::setRootNode
                )
            }
        }
    }
}

private fun onCancelChanges(navController: NavController) {
    navController.popBackStack()
}

private fun onSaveChanges(
    navController: NavController,
    db: AppDatabase,
    node: Node,
    newParentNodeId: Int?,
) {
    CoroutineScope(Dispatchers.Main).launch {
        db.moveNode(node, newParentNodeId)
        navController.popBackStack()
    }
}

private fun canMoveInto(node: Node) {
    return node.
}

@Composable
private fun DirectoryPicker(
    db: AppDatabase,
    animationDirection: Int,
    dirsAndParent: Pair<List<Node>, Node?>,
    node: Node,
    rootNode: Node,
    setRootNode: (Int?, Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val spacing = Preferences.spacingDefault
    val lineHeight = with(density) { fontSize.toDp() }

    BackHandler(enabled = rootNode.parentId != null) { setRootNode(rootNode.parentId, -1) }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        OutlinedReadOnlyValue(
            label = "${node.kind.label} to move",
            modifier = Modifier.fillMaxWidth()
        ) {
            NodePath(db, node)
        }

        OutlinedReadOnlyValue(label = "Destination", modifier = Modifier.fillMaxWidth()) {
            NodePath(db, rootNode)
        }

        AnimatedContent(
            targetState = dirsAndParent,
            label = "directory picker",
            transitionSpec = {
                val animationDuration = 600
                (fadeIn(tween(animationDuration)) +
                    slideInHorizontally(tween(animationDuration)) {
                        it * animationDirection
                    }) togetherWith
                    (fadeOut(tween(animationDuration)) +
                        slideOutHorizontally(tween(animationDuration)) { -it * animationDirection })
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.verticalScrollShadows(spacing / 2)) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing / 2)
                ) {
                    it.first.forEach { node ->
                        var payload by remember { mutableStateOf<Payload?>(null) }
                        LaunchedEffect(node.nodeId) {
                            payload = db.getPayloadByNodeId(node.kind, node.nodeId)
                        }

                        val navigateUp = node.nodeId == (it.second?.nodeId ?: ROOT_NODE_ID)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.padding(vertical = spacing / 2).fillMaxWidth().clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    setRootNode(node.nodeId, if (navigateUp) -1 else 1)
                                }
                        ) {
                            if (navigateUp) {
                                NodeIconAndText(
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    label = "..",
                                    color = Foreground.mix(Background, 0.5f),
                                    icon = Icons.Outlined.DriveFolderUpload
                                )
                            } else {
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
}
