package com.example.mylauncher.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Preferences
import com.example.mylauncher.helper.verticalScrollShadows
import com.example.mylauncher.ui.components.NodeIconAndText
import com.example.mylauncher.ui.components.dialog.YesNoDialog
import com.example.mylauncher.ui.theme.Catppuccin
import com.example.mylauncher.ui.theme.Foreground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

private val extraPadding = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reorder(db: AppDatabase, navController: NavController, nodeId: Int) {
    var parentNode by remember { mutableStateOf<Node?>(null) }
    val nodes = remember { mutableStateOf<List<Node>?>(null) }
    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            parentNode = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Parent node is null")
            // Parent node is added as the first element in the list due to a bug with the
            // reorderable modifier implementation which prevents the first element from being
            // animated properly. See here: https://github.com/aclassen/ComposeReorderable#Notes
            nodes.value = listOf(parentNode!!) + db.nodeDao().getChildNodes(nodeId)
        }
    }

    if (parentNode == null) return

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel reorder",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue reordering",
        noColor = Color(0xFF888888),
        noIcon = Icons.Filled.ArrowBack,
        onYes = {},
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Save new order",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue reordering",
        noColor = Color(0xFF888888),
        noIcon = Icons.Filled.ArrowBack,
        onYes = {},
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(),
                title = {
                    Text(
                        buildAnnotatedString {
                            append("Reordering ")
                            withStyle(SpanStyle(color = parentNode!!.kind.color)) {
                                append(parentNode!!.kind.label)
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
        }
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = extraPadding)
                .verticalScrollShadows(Preferences.spacingDefault)
        ) {
            if (nodes.value != null) {
                ReorderableList(parentNode!!, nodes)
            }
        }
    }
}

@Composable
private fun ReorderableList(parentNode: Node, nodes: MutableState<List<Node>?>) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val spacing = Preferences.spacingDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }

    val state =
        rememberReorderableLazyListState(
            onMove = { from, to ->
                nodes.value =
                    nodes.value!!.toMutableList().apply { add(to.index, removeAt(from.index)) }
            },
            canDragOver = { from, to -> to.index != 0 && from.index != 0 }
        )

    LazyColumn(state = state.listState, modifier = Modifier.reorderable(state).fillMaxWidth()) {
        items(nodes.value!!, { it.nodeId }) { node ->
            ReorderableItem(state, key = node.nodeId, modifier = Modifier.fillMaxWidth()) {
                isDragging ->
                val draggable = remember { node.nodeId != parentNode.nodeId }

                if (draggable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(spacing / 2)
                                .alpha(if (isDragging) 0.65f else 1f),
                    ) {
                        NodeIconAndText(
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            label = node.label,
                            color = node.kind.color,
                            icon = node.kind.icon,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textModifier = Modifier.weight(1f)
                        )

                        Box(Modifier.absolutePadding(left = lineHeight).detectReorder(state)) {
                            Icon(
                                Icons.Filled.DragIndicator,
                                contentDescription = "drag indicator",
                                modifier = Modifier.size(lineHeight),
                                tint = Foreground,
                            )
                        }
                    }
                } else {
                    // Display nothing for the parent node (first element) due to the aforementioned
                    // bug in aclassen/ComposeReorderable. A spacer must be used because zero-height
                    // ReorderableItems cause problems.
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}
