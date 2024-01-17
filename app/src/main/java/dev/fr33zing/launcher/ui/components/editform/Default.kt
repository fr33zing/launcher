package dev.fr33zing.launcher.ui.components.editform

import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.ui.components.node.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments

@Composable
fun DefaultEditForm(arguments: EditFormArguments) {
    EditFormColumn(arguments.padding) { NodePropertyTextField(arguments.node::label) }
}
