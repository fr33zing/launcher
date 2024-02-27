package dev.fr33zing.launcher.ui.components.form.payload

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import dev.fr33zing.launcher.data.persistent.payloads.Note
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments

@Composable
fun NoteEditForm(arguments: EditFormArguments) {
    val (padding, node, payload, creatingNewNode) = arguments
    val notePayload = payload as Note

    EditFormColumn(padding) {
        NodePropertyTextField(node::label, autoFocus = creatingNewNode)
        NodePropertyTextField(notePayload::body, imeAction = ImeAction.Default, minLines = 3)
    }
}
