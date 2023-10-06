package com.example.mylauncher.ui.components

import android.content.Context
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
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.NodeRow
import com.example.mylauncher.data.flattenNodes
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.data.persistent.RelativeNodeOffset
import com.example.mylauncher.data.persistent.RelativeNodePosition
import com.example.mylauncher.data.persistent.createNode
import com.example.mylauncher.helper.launchApp
import com.example.mylauncher.ui.theme.Background
import com.example.mylauncher.ui.theme.Foreground
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val nodesUpdated = PublishSubject.create<Unit>()

fun refreshNodeList() {
    nodesUpdated.onNext(Unit)
}

@Composable
fun NodeList(db: AppDatabase, navController: NavController) {
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
    LaunchedEffect(Unit) { nodes.addAll(flattenNodes(db)) }
    DisposableEffect(Unit) {
        val subscription =
            nodesUpdated.subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    val flatNodes = flattenNodes(db)
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

    Box {
        LazyColumn(state = listState) {
            item { Spacer(Modifier.height(paddingTop)) }

            itemsIndexed(nodes) { index, row ->
                NewNodePositionIndicator(newNodePosition, index, above = true)

                NodeRow(
                    navController,
                    row,
                    nodeOptionsVisibleIndex,
                    index,
                    onTapped = {
                        nodeOptionsVisibleIndex = null
                        onNodeRowTapped(db, context, row)
                    },
                    onLongPressed = { nodeOptionsVisibleIndex = index },
                    onAddNodeDialogOpened = {
                        nodeOptionsVisibleIndex = null
                        newNodePosition = it
                    },
                    onAddNodeDialogClosed = { newNodePosition = null },
                    onNewNodeKindChosen = {
                        nodeOptionsVisibleIndex = null
                        onAddNode(db, navController, newNodePosition!!, it)
                    }
                )

                NewNodePositionIndicator(newNodePosition, index, above = false)
            }

            item { Spacer(Modifier.height(paddingBottom)) }
        }

        TopAndBottomShades(paddingTop, paddingBottom)
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun onNodeRowTapped(db: AppDatabase, context: Context, nodeRow: NodeRow) {
    if (nodeRow.node.kind == NodeKind.Directory) {
        nodeRow.collapsed.value = !nodeRow.collapsed.value
    } else if (nodeRow.node.kind == NodeKind.Application) {
        GlobalScope.launch {
            val app =
                db.applicationDao().getPayloadByNodeId(nodeRow.node.nodeId)
                    ?: throw Exception("Tried to launch null application")
            launchApp(context, app)
        }
    }
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
    index: Int,
    above: Boolean,
) {
    val visible =
        newNodePosition?.relativeToNodeId == index &&
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
                    end = Offset(this.size.width * width.value, 0f),
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
