package dev.fr33zing.launcher.ui.components.form

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.components.tree.old.NodeIconAndText
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Dim
import dev.fr33zing.launcher.ui.utility.conditional
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NodePicker(
    db: AppDatabase,
    initialRootNodeId: Int?,
    selectedNodeState: MutableState<Node?>,
    /** true = visible */
    nodeVisiblePredicate: (Node) -> Boolean = { true },
    /** null = allowed, non-null = blocked with value as reason */
    nodeBlockedReason: suspend (Node) -> String? = { null },
) {
    var rootNode by remember { mutableStateOf<Node?>(null) }
    var rootNodeId by remember { mutableStateOf<Int?>(null) }
    var nodesAndParent by remember { mutableStateOf<Pair<List<Node>, Node?>>(Pair(listOf(), null)) }
    var animationDirection by remember { mutableIntStateOf(-1) }

    fun setRootNode(nodeId: Int?, animDirection: Int, alsoSelectNode: Boolean = true) {
        animationDirection = animDirection

        CoroutineScope(Dispatchers.IO).launch {
            rootNodeId = nodeId
            rootNode =
                db.nodeDao().getNodeById(rootNodeId ?: ROOT_NODE_ID)
                    ?: throw Exception("Root node is null")

            if (alsoSelectNode) selectedNodeState.value = rootNode

            val parentNode = rootNode!!.parentId?.let { db.nodeDao().getNodeById(it) }
            val nodes =
                db.nodeDao()
                    .getChildNodes(rootNode!!.nodeId)
                    .filter(predicate = nodeVisiblePredicate)
            nodesAndParent =
                Pair(
                    if (parentNode != null) listOf(parentNode) + nodes else nodes,
                    parentNode,
                )
        }
    }

    LaunchedEffect(Unit) { setRootNode(initialRootNodeId, 0, false) }

    if (rootNode != null) {
        NodePickerRowList(
            db,
            animationDirection,
            rootNode!!,
            selectedNodeState,
            nodesAndParent,
            ::setRootNode,
            nodeBlockedReason
        )
    }
}

@Composable
private fun NodePickerRowList(
    db: AppDatabase,
    animationDirection: Int,
    rootNode: Node,
    selectedNodeState: MutableState<Node?>,
    nodesAndParent: Pair<List<Node>, Node?>,
    setRootNode: (Int?, Int) -> Unit,
    nodeBlockedReason: suspend (Node) -> String?,
) {
    val preferences = Preferences(LocalContext.current)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fontSize = preferences.nodeAppearance.fontSize.mappedDefault
    val spacing = preferences.nodeAppearance.spacing.mappedDefault
    val lineHeight = with(density) { fontSize.toDp() }

    BackHandler(enabled = rootNode.parentId != null) { setRootNode(rootNode.parentId, -1) }

    AnimatedContent(
        targetState = nodesAndParent,
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
            Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = spacing / 2)) {
                it.first.forEach { node ->
                    NodePickerRow(
                        db,
                        node,
                        rootNode,
                        it.second?.nodeId,
                        selectedNodeState,
                        setRootNode,
                        nodeBlockedReason,
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

@Composable
private fun NodePickerRow(
    db: AppDatabase,
    node: Node,
    rootNode: Node,
    parentNodeId: Int?,
    selectedNodeState: MutableState<Node?>,
    setRootNode: (Int?, Int) -> Unit,
    nodeBlockedReason: suspend (Node) -> String?,
    haptics: HapticFeedback,
    spacing: Dp,
    fontSize: TextUnit,
    lineHeight: Dp,
) {
    var payload by remember { mutableStateOf<Payload?>(null) }
    var blockedMessage by remember { mutableStateOf<String?>(null) }
    val pickable by remember { derivedStateOf { blockedMessage == null } }

    LaunchedEffect(node.nodeId) {
        blockedMessage = nodeBlockedReason(node)
        payload = db.getPayloadByNodeId(node.kind, node.nodeId)
    }

    val navigateUp = node.nodeId == (parentNodeId ?: ROOT_NODE_ID)
    Box(
        Modifier.conditional(!pickable) { alpha(0.5f) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (pickable) {
                            if (node.kind == NodeKind.Directory) {
                                setRootNode(node.nodeId, if (navigateUp) -1 else 1)
                            } else if (node == selectedNodeState.value) {
                                selectedNodeState.value = rootNode
                            } else {
                                selectedNodeState.value = node
                            }
                        } else {
                            sendNotice("move-blocked:${node.nodeId}", blockedMessage!!)
                        }
                    }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = spacing / 2).fillMaxWidth()
        ) {
            if (navigateUp) {
                NodeIconAndText(
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    label = "..",
                    color = Dim,
                    icon = Icons.Outlined.DriveFolderUpload,
                    textModifier = Modifier.weight(1f),
                )
            } else {
                NodeIconAndText(
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    label = node.label,
                    color = node.kind.color(payload, ignoreState = true),
                    icon = node.kind.icon(payload, ignoreState = true),
                    textModifier = Modifier.weight(1f),
                )
            }

            if (node.kind != NodeKind.Directory && node == selectedNodeState.value) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Catppuccin.Current.green
                )
            }
        }
    }
}
