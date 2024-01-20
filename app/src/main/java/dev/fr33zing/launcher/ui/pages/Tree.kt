package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.data.viewmodel.TreeViewModel
import dev.fr33zing.launcher.ui.components.node.next.NodeTree
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun Tree(
    navigateBack: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    NodeTree(viewModel.flow.filterNotNull())
}
