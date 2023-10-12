package dev.fr33zing.launcher.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.NodeRow
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.RelativeNodeOffset
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.persistent.createNode
import dev.fr33zing.launcher.data.persistent.getFlatNodeList
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val nodesUpdated = PublishSubject.create<Unit>()

fun refreshNodeList() {
    nodesUpdated.onNext(Unit)
}

@Composable
fun NodeList(
    db: AppDatabase,
    navController: NavController,
    rootNodeId: Int? = null,
    filter: ((NodeRow) -> Boolean)? = null,
) {
    val paddingTop =
        with(LocalDensity.current) { WindowInsets.statusBars.getTop(LocalDensity.current).toDp() }
    val paddingBottom =
        with(LocalDensity.current) {
            WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp()
        }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val nodes = remember { mutableStateListOf<NodeRow>() }
    var nodeOptionsVisibleIndex by remember { mutableStateOf<Int?>(null) }
    var newNodePosition by remember { mutableStateOf<RelativeNodePosition?>(null) }

    // Populate list and listen for changes
    LaunchedEffect(Unit) { nodes.addAll(db.getFlatNodeList(rootNodeId)) }
    DisposableEffect(Unit) {
        val subscription =
            nodesUpdated.subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    nodeOptionsVisibleIndex = null
                    val flatNodes = db.getFlatNodeList(rootNodeId)
                    nodes.clear()
                    nodes.addAll(flatNodes)
                }
            }

        onDispose { subscription.dispose() }
    }

    // Hide node options when scrolling
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { if (it) nodeOptionsVisibleIndex = null }
    }

    // Hide node options with back button
    BackHandler(nodeOptionsVisibleIndex != null) { nodeOptionsVisibleIndex = null }

    Box {
        LazyColumn(state = listState) {
            item { Spacer(Modifier.height(paddingTop)) }

            itemsIndexed(nodes) { index, row ->
                if (filter == null || filter(row)) {
                    NewNodePositionIndicator(newNodePosition, row.node.nodeId, above = true)

                    NodeRow(
                        db,
                        navController,
                        row,
                        nodeOptionsVisibleIndex,
                        index,
                        onClick = {
                            nodeOptionsVisibleIndex = null
                            onNodeRowClicked(db, context, row)
                        },
                        onLongClick = { nodeOptionsVisibleIndex = index },
                        onAddNodeDialogOpened = {
                            newNodePosition = it
                            nodeOptionsVisibleIndex = null
                        },
                        onAddNodeDialogClosed = { newNodePosition = null },
                        onNewNodeKindChosen = {
                            nodeOptionsVisibleIndex = null
                            onAddNode(db, navController, newNodePosition!!, it)
                        }
                    )

                    NewNodePositionIndicator(newNodePosition, row.node.nodeId, above = false)
                }
            }

            item { Spacer(Modifier.height(paddingBottom)) }
        }

        TopAndBottomShades(paddingTop, paddingBottom)
    }
}

private fun onNodeRowClicked(db: AppDatabase, context: Context, nodeRow: NodeRow) {
    if (nodeRow.node.kind == NodeKind.Directory) nodeRow.collapsed = !nodeRow.collapsed

    nodeRow.payload.activate(db, context)
}

private fun onAddNode(
    db: AppDatabase,
    navController: NavController,
    position: RelativeNodePosition,
    nodeKind: NodeKind
) {
    CoroutineScope(Dispatchers.IO).launch {
        val nodeId = db.createNode(position, nodeKind)
        CoroutineScope(Dispatchers.Main).launch { navController.navigate("create/$nodeId") }
    }
}

@Composable
private fun NewNodePositionIndicator(
    newNodePosition: RelativeNodePosition?,
    nodeId: Int,
    above: Boolean,
) {
    val visible =
        newNodePosition != null &&
            newNodePosition.relativeToNodeId == nodeId &&
            (newNodePosition.offset == RelativeNodeOffset.Above) == above
    val density = LocalDensity.current
    val strokeWidth = with(density) { (2.dp).toPx() }
    val dashInterval = with(density) { (8.dp).toPx() }
    val width =
        animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            label = "new node position indicator width",
        )

    if (visible)
        Box(
            Modifier.fillMaxWidth().drawBehind {
                drawLine(
                    color = Foreground,
                    strokeWidth = strokeWidth,
                    start = Offset(0f, 0f),
                    end = Offset(size.width * width.value, 0f),
                    pathEffect =
                        PathEffect.dashPathEffect(
                            intervals = floatArrayOf(dashInterval, dashInterval),
                            phase = 0f,
                        )
                )
            }
        )
}

/** Gradients to prevent content overlapping the system/navigation bars. */
@Composable
private fun TopAndBottomShades(
    paddingTop: Dp,
    paddingBottom: Dp,
) {
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
        Canvas(
            modifier = Modifier.height(paddingTop).fillMaxWidth(),
            onDraw = {
                drawRect(
                    Brush.verticalGradient(
                        0.0f to Background,
                        1.0f to Color.Transparent,
                        startY = 80.0f,
                        endY = 140.0f,
                    )
                )
            }
        )

        Canvas(
            modifier = Modifier.weight(1f, false).height(paddingBottom).fillMaxWidth(),
            onDraw = {
                drawRect(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        1.0f to Background,
                        startY = 0.0f,
                        endY = 60.0f,
                    )
                )
            }
        )
    }
}
