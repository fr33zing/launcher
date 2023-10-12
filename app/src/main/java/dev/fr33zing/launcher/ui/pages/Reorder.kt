package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.nodeIndent
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.helper.conditional
import dev.fr33zing.launcher.helper.verticalScrollShadows
import dev.fr33zing.launcher.ui.components.CancelButton
import dev.fr33zing.launcher.ui.components.FinishButton
import dev.fr33zing.launcher.ui.components.NodeIconAndText
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.components.refreshNodeList
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.util.mix
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
    val haptics = LocalHapticFeedback.current
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
            nodes.value =
                listOf(parentNode!!) + db.nodeDao().getChildNodes(nodeId).sortedBy { it.order }
        }
    }

    if (parentNode == null || nodes.value == null) return

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel reorder",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue reordering",
        noIcon = Icons.Filled.ArrowBack,
        backAction = YesNoDialogBackAction.Yes,
        onYes = { onCancelChanges(navController) },
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Save new order",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue reordering",
        noIcon = Icons.Filled.ArrowBack,
        onYes = { onSaveChanges(navController, db, nodes.value!!) },
    )

    BackHandler { cancelDialogVisible.value = true }

    Scaffold(
        topBar = {
            TopAppBar(
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
                    CancelButton { cancelDialogVisible.value = true }
                    FinishButton { saveDialogVisible.value = true }
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
            ReorderableList(parentNode!!, nodes)
        }
    }
}

private fun onCancelChanges(navController: NavController) {
    navController.popBackStack()
}

private fun onSaveChanges(navController: NavController, db: AppDatabase, nodes: List<Node>) {
    // Discard first element due to the aforementioned bug.
    val fixedNodes = nodes.subList(1, nodes.size)

    fixedNodes.forEachIndexed { index, node -> node.order = index }
    CoroutineScope(Dispatchers.IO).launch {
        db.nodeDao().updateMany(fixedNodes)
        refreshNodeList()
    }

    navController.popBackStack()
}

@Composable
private fun ReorderableList(parentNode: Node, nodes: MutableState<List<Node>?>) {
    val localDensity = LocalDensity.current
    val fontSize = Preferences.fontSizeDefault
    val spacing = Preferences.spacingDefault
    val lineHeight = with(localDensity) { fontSize.toDp() }
    val indent = remember { nodeIndent(1, Preferences.indentDefault, lineHeight) }

    val listState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(
            listState = listState,
            onMove = { from, to ->
                nodes.value =
                    nodes.value!!.toMutableList().apply { add(to.index, removeAt(from.index)) }
            },
            canDragOver = { from, to -> to.index != 0 && from.index != 0 }
        )

    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier.reorderable(reorderableState).fillMaxWidth()
    ) {
        items(nodes.value!!, { it.nodeId }) { node ->
            ReorderableItem(
                reorderableState,
                key = node.nodeId,
                modifier = Modifier.fillMaxWidth()
            ) { isDragging ->
                val draggable = remember { node.nodeId != parentNode.nodeId }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(spacing / 2)
                            .alpha(if (isDragging) 0.65f else 1f)
                            .conditional(draggable) { absolutePadding(left = indent) },
                ) {
                    NodeIconAndText(
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        label = node.label,
                        color =
                            if (!draggable) Foreground.mix(Background, 0.5f) else node.kind.color,
                        icon = node.kind.icon,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textModifier = Modifier.weight(1f)
                    )

                    if (draggable) {
                        Box(
                            Modifier.absolutePadding(left = lineHeight)
                                .detectReorder(reorderableState)
                        ) {
                            Icon(
                                Icons.Filled.DragIndicator,
                                contentDescription = "drag indicator",
                                modifier = Modifier.size(lineHeight),
                                tint = Foreground,
                            )
                        }
                    }
                }
            }
        }
    }
}
