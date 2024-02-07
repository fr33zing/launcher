package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.TreeNavigator
import dev.fr33zing.launcher.data.viewmodel.TreeViewModel
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.ui.components.tree.NodeActions
import dev.fr33zing.launcher.ui.components.tree.NodeTree

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
        )
    }

    fun activatePayload(treeNodeState: TreeNodeState) {
        viewModel.activatePayload(context, treeNodeState)
    }

    NodeTree(
        treeStateFlow = viewModel.treeStateFlow,
        treeNodeListFlow = viewModel.treeNodeListFlow,
        onDisableFlowStagger = viewModel::disableFlowStagger,
        onActivatePayload = ::activatePayload,
        onSelectNode = viewModel::selectNode,
        onClearSelectedNode = viewModel::clearSelectedNode,
        nodeActions = nodeActions,
    )
}
