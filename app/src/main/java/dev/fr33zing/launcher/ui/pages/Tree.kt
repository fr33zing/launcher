package dev.fr33zing.launcher.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.TreeNavigator
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.viewmodel.ScrollToKeyEvent
import dev.fr33zing.launcher.data.viewmodel.TreeViewModel
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.components.tree.NodeActions
import dev.fr33zing.launcher.ui.components.tree.NodeTree
import dev.fr33zing.launcher.ui.components.tree.modal.ModalActions
import dev.fr33zing.launcher.ui.components.tree.utility.LocalNodeDimensions
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tree(
    navigateBack: () -> Unit,
    navigateTo: TreeNavigator,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val nodeActions = remember {
        NodeActions(
            trash = viewModel::moveNodeToTrash,
            delete = viewModel::deleteNode,
            move = navigateTo.move,
            reorder = navigateTo.reorder,
            edit = navigateTo.edit,
            create = navigateTo.create,
            viewNote = navigateTo.viewNote,
            beginBatchSelect = viewModel::beginBatchSelect,
        )
    }
    val modalBarActions = remember {
        ModalActions(
            endBatchSelect = viewModel::endBatchSelect,
            batchSelectAll = viewModel::batchSelectAll,
            batchDeselectAll = viewModel::batchDeselectAll,
            beginBatchMove = viewModel::beginBatchMove,
            endBatchMove = viewModel::endBatchMove,
        )
    }

    fun activatePayload(treeNodeState: TreeNodeState) =
        viewModel.activatePayload(context, treeNodeState)

    fun createNode(position: RelativeNodePosition, kind: NodeKind) =
        viewModel.createNode(position, kind) { nodeId -> nodeActions.create(nodeId) }

    fun setScrollToKeyCallback(callback: (ScrollToKeyEvent) -> Unit) {
        viewModel.scrollToKeyCallback = callback
    }

    CompositionLocalProvider(
        LocalNodeDimensions provides rememberNodeDimensions(),
        LocalOverscrollConfiguration provides null
    ) {
        NodeTree(
            treeStateFlow = viewModel.treeStateFlow,
            treeNodeListFlow = viewModel.treeNodeListFlow,
            scrollToKeyFlow = viewModel.scrollToKeyFlow,
            highlightKeyFlow = viewModel.highlightKeyFlow,
            onSearch = navigateTo.search,
            setScrollToKeyCallback = ::setScrollToKeyCallback,
            performQueuedScrollToKey = viewModel::performQueuedScrollToKey,
            onDisableFlowStagger = viewModel::disableFlowStagger,
            onActivatePayload = ::activatePayload,
            onSelectNode = viewModel::selectNode,
            onClearSelectedNode = viewModel::clearSelectedNode,
            onToggleNodeBatchSelected = viewModel::toggleNodeBatchSelected,
            onClearHighlightedNode = viewModel::clearHighlightedNode,
            onCreateNode = ::createNode,
            nodeActions = nodeActions,
            modalActions = modalBarActions,
        )
    }
}
