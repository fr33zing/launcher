package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.ReorderViewModel
import dev.fr33zing.launcher.data.viewmodel.sendJumpToNode
import dev.fr33zing.launcher.data.viewmodel.state.ReferenceFollowingNodePayloadState
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.components.form.CancelButton
import dev.fr33zing.launcher.ui.components.form.FinishButton
import dev.fr33zing.launcher.ui.components.tree.NodeDetail
import dev.fr33zing.launcher.ui.components.tree.NodeDetailContainer
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.LocalNodeAppearance
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.rememberNodeAppearance
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

private val extraPadding = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reorder(
    navigateBack: () -> Unit,
    viewModel: ReorderViewModel = hiltViewModel(),
) {
    fun jumpToNode() {
        sendJumpToNode(nodeId = viewModel.childNodeId, snap = false, afterNextUpdate = true)
    }

    fun onCancelChanges() {
        navigateBack()
    }

    fun onSaveChanges() {
        viewModel.saveChanges()
        navigateBack()
        jumpToNode()
    }

    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    val preferences = Preferences(LocalContext.current)
    val askOnAccept by preferences.confirmationDialogs.reorderNodes.askOnAccept.state
    val askOnReject by preferences.confirmationDialogs.reorderNodes.askOnReject.state

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel reorder",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue reordering",
        noIcon = Icons.Filled.ArrowBack,
        backAction = YesNoDialogBackAction.Yes,
        onYes = ::onCancelChanges,
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Save new order",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue reordering",
        noIcon = Icons.Filled.ArrowBack,
        onYes = ::onSaveChanges
    )

    BackHandler { if (askOnReject) cancelDialogVisible.value = true else onCancelChanges() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            append("Reordering ")
                            viewModel.parentNode?.let { parentNode ->
                                withStyle(SpanStyle(color = parentNode.kind.color)) {
                                    append(parentNode.kind.label)
                                }
                            }
                        }
                    )
                },
                actions = {
                    CancelButton {
                        if (askOnReject) cancelDialogVisible.value = true else onCancelChanges()
                    }
                    FinishButton {
                        if (askOnAccept) saveDialogVisible.value = true else onSaveChanges()
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = extraPadding)
                .verticalScrollShadows(preferences.nodeAppearance.spacing.mappedDefault)
        ) {
            if (viewModel.parentNode != null && viewModel.reorderableNodes != null)
                ReorderableList(
                    viewModel.parentNode!!,
                    viewModel.reorderableNodes!!,
                    viewModel::move
                )
        }
    }
}

@Composable
private fun ReorderableList(
    parentNode: Node,
    reorderableNodes: List<ReferenceFollowingNodePayloadState>,
    onMove: (from: Int, to: Int) -> Unit
) {
    val listState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(
            listState = listState,
            onMove = { from, to -> onMove(from.index, to.index) },
            canDragOver = { from, to -> to.index != 0 && from.index != 0 }
        )

    CompositionLocalProvider(LocalNodeDimensions provides rememberNodeDimensions()) {
        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier.reorderable(reorderableState).fillMaxSize()
        ) {
            items(reorderableNodes, { state -> state.underlyingState.node.nodeId }) { state ->
                ReorderableItem(
                    reorderableState,
                    key = state.underlyingState.node.nodeId,
                ) { isDragging ->
                    val draggable = remember {
                        state.underlyingState.node.nodeId != parentNode.nodeId
                    }

                    CompositionLocalProvider(
                        LocalNodeAppearance provides
                            rememberNodeAppearance(state.node, state.payload)
                    ) {
                        NodeDetailContainer(
                            depth = if (draggable) 1 else 0,
                            modifier = Modifier.alpha(if (isDragging) 0.65f else 1f)
                        ) {
                            NodeDetail(
                                label = state.underlyingState.node.label,
                                color =
                                    if (!draggable) Foreground.mix(Background, 0.5f)
                                    else LocalNodeAppearance.current.color,
                                isValidReference = state.isValidReference,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textModifier = Modifier.weight(1f),
                            )

                            if (draggable) {
                                with(LocalNodeDimensions.current) {
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
        }
    }
}
