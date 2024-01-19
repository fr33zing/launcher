package dev.fr33zing.launcher.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.viewmodel.TreeViewModel
import dev.fr33zing.launcher.ui.components.node.next.NodeTree

@Composable
fun Tree(
    navigateBack: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    val state by viewModel.flow.collectAsStateWithLifecycle()

    NodeTree(state ?: emptyList())
}
