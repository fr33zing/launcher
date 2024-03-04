package dev.fr33zing.launcher.ui.components.tree.modal.utility

import androidx.compose.runtime.Immutable
import dev.fr33zing.launcher.data.viewmodel.state.NodeRelevance
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState

@Immutable
data class ModalActions(
    val activatePayload: () -> Unit,
    val selectNode: () -> Unit,
    val clearSelectedNode: () -> Unit,
    val toggleBatchSelected: () -> Unit
)

@Immutable
data class ModalArguments(
    val actions: ModalActions,
    val treeState: TreeState,
    val treeNodeState: TreeNodeState,
    val relevance: NodeRelevance
)
