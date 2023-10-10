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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.checkPermission
import dev.fr33zing.launcher.data.persistent.moveNode
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.helper.conditional
import dev.fr33zing.launcher.helper.verticalScrollShadows
import dev.fr33zing.launcher.ui.components.NodeIconAndText
import dev.fr33zing.launcher.ui.components.NodePath
import dev.fr33zing.launcher.ui.components.OutlinedReadOnlyValue
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val extraPadding = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Move(db: AppDatabase, navController: NavController, nodeId: Int) {
    var movingNode by remember { mutableStateOf<Node?>(null) }
    var movingNodeCurrentParent by remember { mutableStateOf<Node?>(null) }
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
        movingNode = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Node is null")
        movingNodeCurrentParent =
            db.nodeDao().getNodeById(movingNode!!.parentId ?: ROOT_NODE_ID)
                ?: throw Exception("Parent node is null")
        rootNodeId = movingNodeCurrentParent!!.parentId
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
        onYes = { onSaveChanges(navController, db, movingNode!!, rootNodeId) },
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
                            if (movingNode != null) {
                                withStyle(SpanStyle(color = movingNode!!.kind.color)) {
                                    append(movingNode!!.kind.label)
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
        if (movingNode != null && rootNode != null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(extraPadding)) {
                DirectoryPicker(
                    db,
                    animationDirection,
                    dirsAndParent,
                    movingNode!!,
                    movingNodeCurrentParent!!,
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

@Composable
private fun DirectoryPicker(
    db: AppDatabase,
    animationDirection: Int,
    dirsAndParent: Pair<List<Node>, Node?>,
    movingNode: Node,
    movingNodeCurrentParent: Node,
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
            label = "${movingNode.kind.label} to move",
            modifier = Modifier.fillMaxWidth()
        ) {
            NodePath(db, movingNode)
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
                    it.first.forEach { potentialDestinationNode ->
                        if (potentialDestinationNode != movingNode) {
                            DirectoryRow(
                                db,
                                movingNode,
                                movingNodeCurrentParent,
                                potentialDestinationNode,
                                it.second?.nodeId,
                                setRootNode,
                                haptics,
                                spacing,
                                fontSize,
                                lineHeight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryRow(
    db: AppDatabase,
    movingNode: Node,
    movingNodeCurrentParent: Node,
    potentialDestinationNode: Node,
    parentNodeId: Int?,
    setRootNode: (Int?, Int) -> Unit,
    haptics: HapticFeedback,
    spacing: Dp,
    fontSize: TextUnit,
    lineHeight: Dp,
) {
    var payload by remember { mutableStateOf<Payload?>(null) }
    var moveIntoBlockedMessage by remember { mutableStateOf<String?>(null) }
    val canMoveInto by remember { derivedStateOf { moveIntoBlockedMessage == null } }

    LaunchedEffect(potentialDestinationNode.nodeId) {
        payload =
            db.getPayloadByNodeId(potentialDestinationNode.kind, potentialDestinationNode.nodeId)

        if (potentialDestinationNode == movingNodeCurrentParent) {
            val kind =
                movingNode.kind.label.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            moveIntoBlockedMessage =
                "$kind '${movingNode.label}' is already in directory '${potentialDestinationNode.label}'."
        } else if (
            !(db.checkPermission(
                PermissionKind.Move,
                PermissionScope.Recursive,
                potentialDestinationNode
            ) ||
                db.checkPermission(
                    PermissionKind.MoveIn,
                    PermissionScope.Recursive,
                    potentialDestinationNode
                ))
        ) {
            moveIntoBlockedMessage =
                "Cannot move ${movingNode.kind.label.lowercase()} '${movingNode.label}' into directory '${potentialDestinationNode.label}'"
        } else {
            moveIntoBlockedMessage = null
        }
    }

    val navigateUp = potentialDestinationNode.nodeId == (parentNodeId ?: ROOT_NODE_ID)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.padding(vertical = spacing / 2)
                .fillMaxWidth()
                .conditional(canMoveInto) {
                    clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        setRootNode(potentialDestinationNode.nodeId, if (navigateUp) -1 else 1)
                    }
                }
                .conditional(!canMoveInto) {
                    alpha(0.5f).clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        sendNotice(
                            "move-blocked:${potentialDestinationNode.nodeId}",
                            moveIntoBlockedMessage!!
                        )
                    }
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
                label = potentialDestinationNode.label,
                color = potentialDestinationNode.kind.color(payload, ignoreState = true),
                icon = potentialDestinationNode.kind.icon(payload, ignoreState = true),
            )
        }
    }
}
