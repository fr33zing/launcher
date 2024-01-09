package dev.fr33zing.launcher.ui.components.editform

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Note
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.components.node.NodePropertyTextField

@Composable
fun NoteEditForm(innerPadding: PaddingValues, payload: Payload?, node: Node) {
    val notePayload = payload as Note

    EditFormColumn(innerPadding) {
        NodePropertyTextField(node::label)
        NodePropertyTextField(notePayload::body, imeAction = ImeAction.Default, minLines = 3)
    }
}
