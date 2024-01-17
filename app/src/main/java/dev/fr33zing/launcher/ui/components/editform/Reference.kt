package dev.fr33zing.launcher.ui.components.editform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.ui.components.OutlinedValue
import dev.fr33zing.launcher.ui.components.node.NodePicker
import dev.fr33zing.launcher.ui.components.node.NodePropertyTextField

@Composable
fun ReferenceEditForm(
    db: AppDatabase,
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val reference = payload as Reference
    var initialRootNodeId by remember { mutableStateOf<Int?>(null) }
    val selectedNode = remember { mutableStateOf<Node?>(null) }
    val labelState = remember { mutableStateOf(node.label) }

    LaunchedEffect(Unit) {
        if (reference.targetId != null) {
            selectedNode.value = db.nodeDao().getNodeById(reference.targetId!!)
            initialRootNodeId = selectedNode.value?.parentId ?: ROOT_NODE_ID
        } else {
            initialRootNodeId = node.parentId ?: ROOT_NODE_ID
        }
    }

    LaunchedEffect(selectedNode.value) {
        if (selectedNode.value != null) {
            reference.targetId = selectedNode.value!!.nodeId
            node.label = selectedNode.value!!.label
            labelState.value = node.label
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(EditFormSpacing),
        modifier = Modifier.padding(innerPadding).padding(EditFormExtraPadding).fillMaxHeight(),
    ) {
        NodePropertyTextField(node::label, state = labelState)
        OutlinedValue(label = "Target", modifier = Modifier.fillMaxWidth()) { padding ->
            //            if (selectedNode.value != null)
            //                NodePath(db, selectedNode.value!!, modifier =
            // Modifier.padding(padding))
        }
        Box(Modifier.weight(1f)) {
            if (initialRootNodeId != null)
                NodePicker(
                    db,
                    initialRootNodeId,
                    selectedNode,
                    nodeVisiblePredicate = { it != node },
                )
        }
    }
}
