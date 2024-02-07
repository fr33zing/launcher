package dev.fr33zing.launcher.ui.components.form.payload

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.ui.components.form.OutlinedValue
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.utility.rememberCustomIndication

@Composable
fun DirectoryEditForm(arguments: EditFormArguments) {
    val (padding, node, payload) = arguments
    val directory = payload as Directory

    EditFormColumn(padding) {
        val labelState = remember { mutableStateOf(node.label) }
        OutlinedValue(label = "Path", modifier = Modifier.fillMaxWidth()) { padding ->
            //            NodePath(
            //                db,
            //                node,
            //                lastNodeLabelState = labelState,
            //                modifier = Modifier.padding(padding)
            //            )
        }
        NodePropertyTextField(node::label, state = labelState)
        InitialState(directory)
    }
}

@Composable
private fun InitialState(directory: Directory) {
    val radioOptions = Directory.InitialVisibility.values()
    val selectedOption = remember { mutableStateOf(directory.initialVisibility) }

    OutlinedValue(
        "Initial visibility behavior",
        readOnly = false,
        modifier = Modifier.selectableGroup()
    ) { padding ->
        Column {
            radioOptions.forEach { option ->
                InitialStateOption(padding, option, selectedOption.value) {
                    selectedOption.value = it
                    directory.initialVisibility = it
                }
            }
        }
    }
}

@Composable
private fun InitialStateOption(
    padding: PaddingValues,
    option: Directory.InitialVisibility,
    selectedOption: Directory.InitialVisibility,
    onOptionSelected: (Directory.InitialVisibility) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberCustomIndication(color = Catppuccin.Current.pink)
    Box(
        modifier =
            Modifier.selectable(
                selected = (option == selectedOption),
                onClick = { onOptionSelected(option) },
                interactionSource = interactionSource,
                indication = indication,
                role = Role.RadioButton
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(padding).padding(vertical = 10.dp)
        ) {
            RadioButton(selected = (option == selectedOption), onClick = null)
            Text(
                text = option.text(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp),
                color = Foreground
            )
        }
    }
}
