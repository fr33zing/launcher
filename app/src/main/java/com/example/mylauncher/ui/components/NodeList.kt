package com.example.mylauncher.ui.components

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.mylauncher.data.AppDatabase
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.NodeRow
import com.example.mylauncher.data.flattenNodes
import com.example.mylauncher.helper.launchApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun NodeList(db: AppDatabase) {
    val paddingTop = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(LocalDensity.current)
            .toDp()
    }

    val paddingBottom = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(LocalDensity.current)
            .toDp()
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val nodes = remember { mutableStateListOf<NodeRow>() }
    var nodeOptionsVisibleIndex by remember { mutableStateOf<Int?>(null) }

    // Populate list
    LaunchedEffect(Unit) { nodes.addAll(flattenNodes(db)) }

    // Hide node options when scrolling
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect {
            if (it) nodeOptionsVisibleIndex = null
        }
    }

    Box {
        LazyColumn(state = listState) {
            item { Spacer(Modifier.height(paddingTop)) }

            itemsIndexed(nodes) { index, row ->
                NodeRow(row, nodeOptionsVisibleIndex, index, onTapped = {
                    nodeOptionsVisibleIndex = null
                    onNodeRowTapped(db, context, row)
                }, onLongPressed = {
                    nodeOptionsVisibleIndex = index
                })
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
    } else if (nodeRow.node.kind == NodeKind.App) {
        GlobalScope.launch {
            val app = db.nodeDao()
                .getApp(nodeRow.node.nodeId)
            launchApp(context, app)
        }
    }
}

/** Gradients to prevent content overlapping the system/navigation bars. */
@Composable
private fun TopAndBottomShades(
    paddingTop: Dp,
    paddingBottom: Dp,
) {
    Column(
        Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween
    ) {
        Canvas(modifier = Modifier
            .height(paddingTop)
            .fillMaxWidth(), onDraw = {
            drawRect(
                Brush.verticalGradient(
                    0.0f to Color.Black,
                    1.0f to Color.Transparent,
                    startY = 80.0f,
                    endY = 140.0f
                )
            )
        })

        Canvas(modifier = Modifier
            .weight(1f, false)
            .height(paddingBottom)
            .fillMaxWidth(), onDraw = {
            drawRect(
                Brush.verticalGradient(
                    0.0f to Color.Transparent, 1.0f to Color.Black, startY = 0.0f, endY = 60.0f
                )
            )
        })
    }
}

