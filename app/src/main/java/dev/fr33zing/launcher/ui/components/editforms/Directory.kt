package dev.fr33zing.launcher.ui.components.editforms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.components.EditFormColumn
import dev.fr33zing.launcher.ui.components.NodePropertyTextField

// TODO implement this

@Composable
fun DirectoryEditForm(
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val directory = payload as Directory

    EditFormColumn(innerPadding) {
        NodePropertyTextField(node::label)
        Spacer(Modifier.height(16.dp))
        InitialState()
    }
}

@Composable
private fun InitialState() {
    val radioOptions = Directory.InitialState.values()
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
    Column(Modifier.selectableGroup()) {
        Text("Initial visibility behavior")
        radioOptions.forEach { option ->
            InitialStateOption(option, selectedOption, onOptionSelected)
        }
    }
}

@Composable
private fun InitialStateOption(
    option: Directory.InitialState,
    selectedOption: Directory.InitialState,
    onOptionSelected: (Directory.InitialState) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = (option == selectedOption),
                onClick = { onOptionSelected(option) },
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (option == selectedOption),
            onClick = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = option.text(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
