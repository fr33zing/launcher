package dev.fr33zing.launcher.ui.components.form.payload

import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments

@Composable
fun DefaultEditForm(arguments: EditFormArguments) {
    EditFormColumn(arguments.padding) { NodePropertyTextField(arguments.node::label) }
}
