package dev.fr33zing.launcher.ui.components.editforms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.ui.components.EditFormColumn
import dev.fr33zing.launcher.ui.components.NodePropertyTextField

@Composable
fun DefaultEditForm(innerPadding: PaddingValues, node: Node) {
    EditFormColumn(innerPadding) { NodePropertyTextField(node::label) }
}