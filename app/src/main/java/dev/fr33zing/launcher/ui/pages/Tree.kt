package dev.fr33zing.launcher.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.fr33zing.launcher.ui.components.tree.utility.NodeRowFeatures
import dev.fr33zing.launcher.ui.components.tree.utility.NodeTreeAppearAnimations
import dev.fr33zing.launcher.ui.components.tree.utility.NodeTreeHighlightAnimation
import dev.fr33zing.launcher.ui.components.tree.utility.rememberNodeDimensions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tree(
    navigateBack: () -> Unit,
    navigateTo: TreeNavigator,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val nodeActions =
        remember {
            NodeActions(
                trash = viewModel::moveNodeToTrash,
                delete = viewModel::deleteNode,
                move = viewModel::beginMove,
                reorder = navigateTo.reorder,
                edit = navigateTo.edit,
                create = navigateTo.create,
                viewNote = navigateTo.viewNote,
                beginBatchSelect = viewModel::beginBatchSelect,
            )
        }
    val modalActions =
        remember {
            ModalActions(
                endBatchSelect = viewModel::endBatchSelect,
                batchSelectAll = viewModel::batchSelectAll,
                batchDeselectAll = viewModel::batchDeselectAll,
                beginMove = viewModel::beginMove,
                cancelMove = viewModel::cancelMove,
            )
        }
    val features = remember { NodeRowFeatures.All }
    val appearAnimations =
        remember {
            NodeTreeAppearAnimations(
                features = features,
                coroutineScope = coroutineScope,
                treeNodeListFlow = viewModel.treeNodeListFlow,
            )
        }
    val highlightAnimations =
        remember {
            NodeTreeHighlightAnimation(
                onDisableFlowStagger = viewModel::disableFlowStagger,
                onClearHighlightedNode = viewModel::clearHighlightedNode,
                coroutineScope = coroutineScope,
                highlightKeyFlow = viewModel.highlightKeyFlow,
            )
        }

    fun activatePayload(treeNodeState: TreeNodeState) = viewModel.activatePayload(context, treeNodeState)

    fun createNode(
        position: RelativeNodePosition,
        kind: NodeKind,
    ) = viewModel.createNode(position, kind) { nodeId -> nodeActions.create(nodeId) }

    fun setScrollToKeyCallback(callback: (ScrollToKeyEvent) -> Unit) {
        viewModel.scrollToKeyCallback = callback
    }

    CompositionLocalProvider(
        LocalNodeDimensions provides rememberNodeDimensions(),
        LocalOverscrollConfiguration provides null,
    ) {
        NodeTree(
            treeStateFlow = viewModel.treeStateFlow,
            animatedTreeNodeListFlow = appearAnimations.flow,
            scrollToKeyFlow = viewModel.scrollToKeyFlow,
            highlightKeyFlow = highlightAnimations.flow,
            highlightAlpha = highlightAnimations.alpha,
            onSearch = navigateTo.search,
            setScrollToKeyCallback = ::setScrollToKeyCallback,
            performQueuedScrollToKey = viewModel::performQueuedScrollToKey,
            onDisableFlowStagger = viewModel::disableFlowStagger,
            onActivatePayload = ::activatePayload,
            onSelectNode = viewModel::selectNode,
            onClearSelectedNode = viewModel::clearSelectedNode,
            onToggleNodeBatchSelected = viewModel::toggleNodeBatchSelected,
            onMoveBatchSelectedNodes = viewModel::confirmMove,
            onClearHighlightedNode = viewModel::clearHighlightedNode,
            onCreateNode = ::createNode,
            nodeActions = nodeActions,
            modalActions = modalActions,
        )
    }
}
