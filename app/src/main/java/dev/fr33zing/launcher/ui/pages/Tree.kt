package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.data.viewmodel.TreeViewModel
import dev.fr33zing.launcher.data.viewmodel.utility.TreeNodeState
import dev.fr33zing.launcher.ui.components.node.next.NodeTree
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun Tree(
    navigateBack: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    fun activatePayload(treeNodeState: TreeNodeState) {
        viewModel.activatePayload(context, treeNodeState)
    }

    NodeTree(
        ::activatePayload,
        viewModel.flow.filterNotNull(),
    )
}
