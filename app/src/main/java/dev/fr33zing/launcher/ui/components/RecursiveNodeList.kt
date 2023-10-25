package dev.fr33zing.launcher.ui.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.TAG
import dev.fr33zing.launcher.data.AllPermissions
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionMap
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.clone
import dev.fr33zing.launcher.data.hasPermission
import dev.fr33zing.launcher.data.nodeIndent
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.NodeCreatedSubject
import dev.fr33zing.launcher.data.persistent.NodeDeletedSubject
import dev.fr33zing.launcher.data.persistent.NodeMovedSubject
import dev.fr33zing.launcher.data.persistent.NodeUpdatedSubject
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.RelativeNodeOffset
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.persistent.createNode
import dev.fr33zing.launcher.data.persistent.deleteRecursively
import dev.fr33zing.launcher.data.persistent.moveToTrash
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.helper.detectZoom
import dev.fr33zing.launcher.helper.verticalScrollShadows
import dev.fr33zing.launcher.ui.components.dialog.AddNodeDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.rememberCustomIndication
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.Float.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val RecursiveNodeListHorizontalPadding = 16.dp
private val closeNodeOptionsSubject = PublishSubject.create<Unit>()

private data class NodeListDimensions(
    val fontSize: TextUnit,
    val spacing: Dp,
    val indent: Dp,
    val lineHeight: Dp,
)

@Composable
private fun rememberNodeListDimensions(
    scale: MutableFloatState,
): NodeListDimensions {
    val preferences = Preferences(LocalContext.current)
    val localDensity = LocalDensity.current
    val unscaledFontSize by preferences.getFontSize()
    val unscaledSpacing by preferences.getSpacing()
    val unscaleIndent by preferences.getIndent()

    return remember(localDensity, scale.floatValue) {
        val fontSize = unscaledFontSize * scale.floatValue
        val spacing = unscaledSpacing * scale.floatValue
        val indent = unscaleIndent * scale.floatValue
        val lineHeight = with(localDensity) { fontSize.toDp() }

        NodeListDimensions(
            fontSize = fontSize,
            spacing = spacing,
            indent = indent,
            lineHeight = lineHeight,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ZoomDetector(
    scaleState: MutableFloatState,
    min: Float = 0.6f,
    max: Float = 1.0f,
    changeScale: Float = 1f,
) {
    Box(
        Modifier.fillMaxSize()
            .pointerInteropFilter { false }
            .pointerInput(Unit) {
                detectZoom(changeScale) { change ->
                    scaleState.floatValue = max(min, min(scaleState.floatValue * change, max))
                }
            }
    )
}

@Composable
fun NodeIconAndText(
    fontSize: TextUnit,
    lineHeight: Dp,
    label: String,
    color: Color,
    icon: ImageVector,
    lineThrough: Boolean = false,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Visible,
    @SuppressLint("ModifierParameter") textModifier: Modifier = Modifier
) {
    val iconSize = remember { 1f }
    Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(lineHeight * iconSize),
    )
    Text(
        text = label,
        modifier =
            Modifier.offset(y = lineHeight * -0.1f) // HACK: Vertically align with icon
                .absolutePadding(left = lineHeight * 0.5f)
                .then(textModifier),
        color = color,
        fontSize = fontSize,
        softWrap = softWrap,
        overflow = overflow,
        textDecoration = if (lineThrough) TextDecoration.LineThrough else null
    )
}

@Composable
fun RecursiveNodeListSetup(
    db: AppDatabase,
    navController: NavController,
    rootNodeId: Int?,
    scrollState: ScrollState,
    shadowHeight: Dp,
) {
    //
    // State
    //

    val context = LocalContext.current
    val scale = remember { mutableFloatStateOf(1f) }
    val dimensions = rememberNodeListDimensions(scale)
    var optionsVisibleNodeId by remember { mutableStateOf<Int?>(null) }
    var newNodePosition by remember { mutableStateOf<RelativeNodePosition?>(null) }

    var rootNode by remember { mutableStateOf<Node?>(null) }
    var payload by remember { mutableStateOf<Payload?>(null) }
    val permissions = remember { AllPermissions.clone() }

    //
    // Interaction
    //

    fun hideNodeOptions() {
        optionsVisibleNodeId = null
    }

    fun showNodeOptions(nodeId: Int?) {
        optionsVisibleNodeId = nodeId
    }

    DisposableEffect(Unit) {
        val subscriptions =
            listOf(
                closeNodeOptionsSubject.subscribe { hideNodeOptions() },
                NodeMovedSubject.subscribe { hideNodeOptions() }
            )
        onDispose { subscriptions.forEach(Disposable::dispose) }
    }

    BackHandler(enabled = optionsVisibleNodeId != null) { hideNodeOptions() }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) hideNodeOptions() }
    }

    fun onNodeClick(payload: Payload) {
        hideNodeOptions()
        payload.activate(db, context)
    }

    fun onNodeLongClick(node: Node) {
        showNodeOptions(node.nodeId)
    }

    fun onNodeChildrenVisibilityChange(payload: Payload, visible: Boolean) {
        val directory = payload as? Directory ?: throw Exception("Payload is not Directory")
        directory.collapsed = !visible
        CoroutineScope(Dispatchers.IO).launch { db.update(directory) }
    }

    fun onAddNodeDialogOpened(relativeNodePosition: RelativeNodePosition) {
        Log.d(TAG, "Opened add node dialog, relative position: $relativeNodePosition")
        newNodePosition = relativeNodePosition
        hideNodeOptions()
    }

    fun onAddNodeDialogClosed() {
        newNodePosition = null
    }

    fun onAddNode(nodeKind: NodeKind) {
        CoroutineScope(Dispatchers.IO).launch {
            val position = newNodePosition ?: throw Exception("newNodePosition is null")
            val nodeId = db.createNode(position, nodeKind)
            CoroutineScope(Dispatchers.Main).launch { navController.navigate("create/$nodeId") }
        }
    }

    //
    // Rendering
    //

    LaunchedEffect(rootNodeId) {
        (rootNodeId ?: ROOT_NODE_ID).let { nodeId ->
            rootNode = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Node is null")
            payload = db.getPayloadByNodeId(rootNode!!.kind, nodeId)
        }
    }

    Box(Modifier.verticalScrollShadows(shadowHeight)) {
        if (rootNode != null && payload != null) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(vertical = shadowHeight)
            ) {
                RecursiveNodeList(
                    db = db,
                    navController = navController,
                    depth = -1,
                    node = rootNode!!,
                    payload = payload!!,
                    permissions = permissions,
                    dimensions = dimensions,
                    optionsVisibleNodeId = optionsVisibleNodeId,
                    onNodeClick = ::onNodeClick,
                    onNodeLongClick = ::onNodeLongClick,
                    onNodeChildrenVisibilityChange = ::onNodeChildrenVisibilityChange,
                    onAddNodeDialogOpened = ::onAddNodeDialogOpened,
                    onAddNodeDialogClosed = ::onAddNodeDialogClosed,
                    onAddNode = ::onAddNode
                )
            }
        }
        ZoomDetector(scaleState = scale)
    }
}

@Composable
private fun RecursiveNodeList(
    db: AppDatabase,
    navController: NavController,
    depth: Int,
    node: Node,
    payload: Payload,
    permissions: PermissionMap,
    dimensions: NodeListDimensions,
    optionsVisibleNodeId: Int?,
    onNodeClick: (Payload) -> Unit,
    onNodeLongClick: (Node) -> Unit,
    onNodeChildrenVisibilityChange: (Payload, Boolean) -> Unit,
    onAddNodeDialogOpened: (RelativeNodePosition) -> Unit,
    onAddNodeDialogClosed: () -> Unit,
    onAddNode: (NodeKind) -> Unit,
) {
    val canHaveChildren = remember { payload is Directory }
    val children = remember { mutableStateListOf<Pair<Node, Payload>>() }
    var childrenVisible by remember {
        mutableStateOf(if (payload is Directory) !payload.initiallyCollapsed else false)
    }
    val childrenVisibleTransition = remember { MutableTransitionState(childrenVisible) }

    // Determine permissions.
    val ownPermissions =
        remember(payload, permissions) {
            val p = permissions.clone()
            if (payload is Directory)
                PermissionKind.values().forEach { kind ->
                    PermissionScope.values().forEach { scope ->
                        if (!payload.hasPermission(kind, scope)) p[kind]!!.remove(scope)
                    }
                }
            p
        }
    val childPermissions =
        remember(payload, permissions) {
            val p = permissions.clone()
            if (payload is Directory)
                PermissionKind.values().forEach { kind ->
                    if (!payload.hasPermission(kind, PermissionScope.Recursive)) {
                        p[kind]!!.remove(PermissionScope.Self)
                        p[kind]!!.remove(PermissionScope.Recursive)
                    }
                }
            p
        }

    // Render self.
    if (node.nodeId != ROOT_NODE_ID) {
        RecursiveNodeListRow(
            db = db,
            navController = navController,
            depth = depth,
            node = node,
            payload = payload,
            childrenVisible = childrenVisible,
            parentPermissions = permissions,
            ownPermissions = ownPermissions,
            dimensions = dimensions,
            optionsVisible = optionsVisibleNodeId == node.nodeId,
            onClick = {
                if (canHaveChildren) {
                    childrenVisible = !childrenVisible
                    onNodeChildrenVisibilityChange(payload, childrenVisible)
                }
                onNodeClick(payload)
            },
            onLongClick = { onNodeLongClick(node) },
            onAddNodeDialogOpened = onAddNodeDialogOpened,
            onAddNodeDialogClosed = onAddNodeDialogClosed,
            onAddNode = onAddNode,
        )
    }

    if (canHaveChildren) {
        LaunchedEffect(childrenVisible) {
            // Update directory collapsed in database.
            if (payload is Directory) {
                if (payload.initialVisibility == Directory.InitialVisibility.Remember) {
                    payload.collapsed = !childrenVisible
                    db.update(payload)
                }
            } else throw Exception("Payload is not a Directory")

            // Load children when expand animation starts.
            childrenVisibleTransition.targetState = childrenVisible
            if (childrenVisible)
                db.nodeDao()
                    .getChildNodes(node.nodeId)
                    .mapNotNull { childNode ->
                        db.getPayloadByNodeId(childNode.kind, childNode.nodeId)?.let { childPayload
                            ->
                            Pair(childNode, childPayload)
                        }
                    }
                    .sortedBy { it.first.order }
                    .let { result ->
                        result.forEachIndexed { index, child ->
                            if (children.size > index)
                                children.removeAt(index) // This prevents duplication.
                            children.add(index, child)
                            if (index == result.size) childrenVisibleTransition.targetState = true
                            else delay(1) // This prevents a lag spike.
                        }
                    }
        }

        // Unload children when collapse animation finishes.
        with(childrenVisibleTransition) {
            LaunchedEffect(isIdle) { if (isIdle && !currentState) children.clear() }
        }

        // React to database changes.
        // TODO fix order after changes
        DisposableEffect(node) {
            fun log(vararg lines: String) {
                Log.i(TAG, "[DB] ${lines.joinToString("\n ->      ")}")
            }

            val subscriptions =
                listOf(
                    NodeCreatedSubject.subscribe { event ->
                        val (createdNodeId, parentNodeId) = event
                        if (node.nodeId == parentNodeId)
                            CoroutineScope(Dispatchers.IO).launch {
                                val createdNode =
                                    db.nodeDao().getNodeById(createdNodeId)
                                        ?: throw Exception("Node is null")
                                val createdPayload =
                                    db.getPayloadByNodeId(createdNode.kind, createdNodeId)
                                        ?: throw Exception("Payload is null")
                                children.add(createdNode.order, Pair(createdNode, createdPayload))
                                log("Created node: $createdNode", "Parent node: $node")
                            }
                    },
                    NodeUpdatedSubject.subscribe { event ->
                        val (updatedNodeId, parentNodeId) = event
                        if (node.nodeId == parentNodeId) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val updatedNode =
                                    db.nodeDao().getNodeById(updatedNodeId)
                                        ?: throw Exception("Node is null")
                                val updatedPayload =
                                    db.getPayloadByNodeId(updatedNode.kind, updatedNodeId)
                                        ?: throw Exception("Payload is null")
                                children
                                    .indexOfFirst { childPair ->
                                        childPair.first.nodeId == updatedNodeId
                                    }
                                    .let { index ->
                                        if (index == -1) return@let
                                        children.removeAt(index)
                                        children.add(index, Pair(updatedNode, updatedPayload))
                                        log("Updated node: $updatedNode", "Parent node: $node")
                                    }
                            }
                        }
                    },
                    NodeDeletedSubject.subscribe { event ->
                        val (deletedNodeId, parentNodeId) = event
                        if (node.nodeId == parentNodeId)
                            children
                                .indexOfFirst { childPair ->
                                    childPair.first.nodeId == deletedNodeId
                                }
                                .let { index ->
                                    if (index == -1) return@let
                                    val (deletedNode) = children.removeAt(index)
                                    log("Deleted node: $deletedNode", "Parent node: $node")
                                }
                    },
                    NodeMovedSubject.subscribe { event ->
                        val (movedNodeId, fromParentNodeId, toParentNodeId) = event
                        if (node.nodeId == fromParentNodeId)
                            children
                                .indexOfFirst { childPair -> childPair.first.nodeId == movedNodeId }
                                .let { index ->
                                    if (index == -1) return@let
                                    val (movedNode) = children.removeAt(index)
                                    log("(1/2) Moved node: $movedNode", "From node: $node")
                                }
                        if (node.nodeId == toParentNodeId)
                            CoroutineScope(Dispatchers.IO).launch {
                                val movedNode =
                                    db.nodeDao().getNodeById(movedNodeId)
                                        ?: throw Exception("Node is null")
                                val movedPayload =
                                    db.getPayloadByNodeId(movedNode.kind, movedNodeId)
                                        ?: throw Exception("Payload is null")
                                children.add(movedNode.order, Pair(movedNode, movedPayload))
                                log("(2/2) Moved node: $movedNode", "To node: $node")
                            }
                    },
                )

            Log.v(TAG, "+ Node subscribed to database events: $node")
            onDispose {
                Log.v(TAG, "- Node disposed subscriptions to database events: $node")
                subscriptions.forEach(Disposable::dispose)
            }
        }

        // Render children.
        AnimatedVisibility(
            visibleState = childrenVisibleTransition,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                for (childPair in children) {
                    key(childPair) {
                        RecursiveNodeList(
                            db = db,
                            navController = navController,
                            depth = depth + 1,
                            node = childPair.first,
                            payload = childPair.second,
                            permissions = childPermissions,
                            dimensions = dimensions,
                            optionsVisibleNodeId = optionsVisibleNodeId,
                            onNodeClick = onNodeClick,
                            onNodeLongClick = onNodeLongClick,
                            onNodeChildrenVisibilityChange = onNodeChildrenVisibilityChange,
                            onAddNodeDialogOpened = onAddNodeDialogOpened,
                            onAddNodeDialogClosed = onAddNodeDialogClosed,
                            onAddNode = onAddNode,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecursiveNodeListRow(
    db: AppDatabase,
    navController: NavController,
    depth: Int,
    node: Node,
    payload: Payload,
    childrenVisible: Boolean,
    parentPermissions: PermissionMap,
    ownPermissions: PermissionMap,
    dimensions: NodeListDimensions,
    optionsVisible: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddNodeDialogOpened: (RelativeNodePosition) -> Unit,
    onAddNodeDialogClosed: () -> Unit,
    onAddNode: (NodeKind) -> Unit,
) {
    if (payload is Directory) payload.collapsed = !childrenVisible

    val label = remember(node) { node.label }
    val color = remember(payload, childrenVisible) { node.kind.color(payload) }
    val icon = remember(payload, childrenVisible) { node.kind.icon(payload) }
    val lineThrough = remember(payload) { node.kind.lineThrough(payload) }

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color, longPressable = true)

    val userCanCreateWithin =
        remember(ownPermissions) {
            ownPermissions.hasPermission(PermissionKind.Create, PermissionScope.Recursive)
        }
    val userCanCreateWithinParent =
        remember(ownPermissions) {
            parentPermissions.hasPermission(PermissionKind.Create, PermissionScope.Recursive)
        }
    val isExpandedDirectory =
        remember(node.kind, childrenVisible) { node.kind == NodeKind.Directory && childrenVisible }
    val showCreateBelowButton =
        if (isExpandedDirectory) userCanCreateWithin else userCanCreateWithinParent

    val rowModifier =
        remember(depth, dimensions.spacing, dimensions.indent) {
            Modifier.padding(
                    vertical = dimensions.spacing / 2,
                    horizontal = RecursiveNodeListHorizontalPadding,
                )
                .padding(start = dimensions.indent * depth)
                .fillMaxWidth()
        }

    Column {
        if (userCanCreateWithinParent) {
            AddNodeButton(
                dimensions = dimensions,
                depth = depth,
                visible = optionsVisible,
                below = false,
                text = "Add item above",
                onDialogOpened = {
                    onAddNodeDialogOpened(
                        RelativeNodePosition(node.nodeId, RelativeNodeOffset.Above)
                    )
                },
                onDialogClosed = onAddNodeDialogClosed,
                onKindChosen = onAddNode,
            )
        }

        Box(
            Modifier.height(IntrinsicSize.Min)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = rowModifier) {
                NodeIconAndText(
                    fontSize = dimensions.fontSize,
                    lineHeight = dimensions.lineHeight,
                    label = label,
                    color = color,
                    icon = icon,
                    lineThrough = lineThrough,
                )
            }
            this@Column.AnimatedVisibility(
                visible = optionsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NodeOptionButtons(
                    db = db,
                    navController = navController,
                    node = node,
                    payload = payload,
                    permissions = ownPermissions,
                    fontSize = dimensions.fontSize,
                    lineHeight = dimensions.lineHeight,
                )
            }
        }

        if (showCreateBelowButton) {
            AddNodeButton(
                dimensions = dimensions,
                depth = if (isExpandedDirectory) depth + 1 else depth,
                visible = optionsVisible,
                below = true,
                text = if (isExpandedDirectory) "Add item within" else "Add item below",
                onDialogOpened = {
                    onAddNodeDialogOpened(
                        RelativeNodePosition(
                            node.nodeId,
                            if (isExpandedDirectory) RelativeNodeOffset.Within
                            else RelativeNodeOffset.Below
                        )
                    )
                },
                onDialogClosed = onAddNodeDialogClosed,
                onKindChosen = onAddNode,
            )
        }
    }
}

@Composable
private fun NodeOptionButtons(
    db: AppDatabase,
    navController: NavController,
    node: Node,
    payload: Payload,
    permissions: PermissionMap,
    fontSize: TextUnit,
    lineHeight: Dp,
) {
    val showTrashButton =
        remember(permissions) {
            permissions.hasPermission(PermissionKind.Delete, PermissionScope.Self)
        }
    val showEmptyTrashButton =
        remember(permissions) {
            payload is Directory && payload.specialMode == Directory.SpecialMode.Trash
        }
    val showMoveButton =
        remember(permissions) {
            permissions.hasPermission(PermissionKind.Move, PermissionScope.Self) ||
                permissions.hasPermission(PermissionKind.MoveIn, PermissionScope.Self) ||
                permissions.hasPermission(PermissionKind.MoveOut, PermissionScope.Self)
        }
    val showReorderButton = remember(permissions) { true }
    val showEditButton =
        remember(permissions) {
            permissions.hasPermission(PermissionKind.Edit, PermissionScope.Self)
        }
    val showInfoButton = remember(permissions) { node.kind == NodeKind.Application }

    NodeOptionButtonsLayout(
        Modifier.fillMaxHeight()
            .background(Background.copy(alpha = 0.75f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { /* Prevent tapping node underneath */}
            )
    ) {
        if (showTrashButton)
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Delete, "Trash") {
                sendNotice(
                    "moved-to-trash:${node.nodeId}",
                    "Moved ${node.kind.label.lowercase()} '${node.label}' to the trash."
                )
                CoroutineScope(Dispatchers.IO).launch { db.moveToTrash(node) }
            }

        if (showEmptyTrashButton) {
            val emptyTrashDialogVisible = remember { mutableStateOf(false) }
            YesNoDialog(
                visible = emptyTrashDialogVisible,
                icon = Icons.Outlined.DeleteForever,
                yesText = "Delete trash forever",
                yesColor = Catppuccin.Current.red,
                yesIcon = Icons.Filled.Dangerous,
                noText = "Don't empty trash",
                noIcon = Icons.Filled.ArrowBack,
                onYes = {
                    Log.d(TAG, "User requested recursive deletion for node: $node")
                    CoroutineScope(Dispatchers.IO).launch { db.deleteRecursively(node) }
                },
            )
            NodeOptionButton(
                fontSize,
                lineHeight,
                Icons.Outlined.DeleteForever,
                "Empty",
                color = Catppuccin.Current.red
            ) {
                emptyTrashDialogVisible.value = true
            }
        }

        if (showMoveButton) {
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.DriveFileMove, "Move") {
                navController.navigate("move/${node.nodeId}")
            }
        }

        if (showReorderButton) {
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.SwapVert, "Reorder") {
                navController.navigate("reorder/${node.parentId}")
            }
        }

        if (showEditButton) {
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Edit, "Edit") {
                navController.navigate("edit/${node.nodeId}")
            }
        }

        if (showInfoButton) {
            val context = LocalContext.current
            NodeOptionButton(fontSize, lineHeight, Icons.Outlined.Info, "Info") {
                closeNodeOptionsSubject.onNext(Unit)
                (payload as Application).openInfo(context)
            }
        }
    }
}

@Composable
private fun NodeOptionButton(
    fontSize: TextUnit,
    lineHeight: Dp,
    icon: ImageVector,
    text: String,
    color: Color = Foreground,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = color, circular = true)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.clickable(interactionSource = interactionSource, indication = indication) {
                Log.d(TAG, "Node option button clicked: $text")
                onClick()
            }
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(lineHeight * 0.125f, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.aspectRatio(1f, true)
        ) {
            Icon(
                icon,
                text,
                tint = color,
                modifier = Modifier.size(lineHeight * 1.15f),
            )
            Text(
                text,
                fontSize = fontSize * 0.65f,
                fontWeight = FontWeight.Bold,
                color = color,
                overflow = TextOverflow.Visible,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun NodeOptionButtonsLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.minHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x =
                        ((constraints.maxWidth / placeables.size * (index + 0.5f)) -
                                placeable.width / 2)
                            .toInt(),
                    y = 0
                )
            }
        }
    }
}

@Composable
private fun AddNodeButton(
    dimensions: NodeListDimensions,
    depth: Int,
    visible: Boolean,
    below: Boolean,
    text: String,
    onDialogOpened: () -> Unit,
    onDialogClosed: () -> Unit,
    onKindChosen: (NodeKind) -> Unit,
) {
    val color = remember { Foreground.copy(alpha = 0.5f) }
    val expandFrom = remember(below) { if (below) Alignment.Bottom else Alignment.Top }
    val dialogVisible = remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color)

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = expandFrom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = expandFrom) + fadeOut(),
        modifier =
            Modifier.fillMaxWidth().clickable(interactionSource, indication, enabled = visible) {
                dialogVisible.value = true
                onDialogOpened()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(vertical = dimensions.spacing / 2)
                    .absolutePadding(
                        left = nodeIndent(depth, dimensions.indent, dimensions.lineHeight)
                    )
        ) {
            NodeIconAndText(
                fontSize = dimensions.fontSize,
                lineHeight = dimensions.lineHeight,
                label = text,
                color = color,
                icon = Icons.Outlined.Add
            )
        }
    }

    AddNodeDialog(
        visible = dialogVisible,
        onDismissRequest = onDialogClosed,
        onKindChosen = {
            dialogVisible.value = false
            onKindChosen(it)
        }
    )
}
