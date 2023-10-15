package dev.fr33zing.launcher.ui.components.editforms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Location
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.components.EditFormColumn
import dev.fr33zing.launcher.ui.components.NodePropertyTextField
import dev.fr33zing.launcher.ui.theme.outlinedTextFieldColors
import kotlin.text.Typography.degree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO support more coordinate formats and cleanup detection code
private val osmandCoordinatesRegex =
    "\\d+(\\.\\d+)?$degree?\\s?[NS]?,\\s?\\d+(\\.\\d+)?$degree?\\s?[EW]?".toRegex()

private fun clipboardLocation(clipboard: ClipboardManager): String? {
    if (!clipboard.hasText()) return null
    val copiedText = clipboard.getText()!!.text.trim()
    if (!osmandCoordinatesRegex.matches(copiedText)) return null

    val coordinates = copiedText.replace(degree.toString(), "").replace(" ", "").split(",")
    if (coordinates.size != 2) return null

    var latitude = coordinates[0].trim()
    if (latitude.endsWith("N")) {
        latitude = latitude.dropLast(1)
    } else if (latitude.endsWith("S")) {
        latitude = "-" + latitude.dropLast(1)
    }

    var longitude = coordinates[1].trim()
    if (longitude.endsWith("E")) {
        longitude = longitude.dropLast(1)
    } else if (longitude.endsWith("W")) {
        longitude = "-" + longitude.dropLast(1)
    }

    return "$latitude,$longitude"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LocationEditForm(
    innerPadding: PaddingValues,
    payload: Payload?,
    node: Node,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val location = payload as Location
    val geoUriState = remember { mutableStateOf(location.geoUri) }
    var inputsEnabled by remember { mutableStateOf(true) }

    fun clearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()

        // HACK: Quickly disable and enable to clear selection
        inputsEnabled = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(25)
            inputsEnabled = true
        }
    }

    EditFormColumn(innerPadding) {
        NodePropertyTextField(node::label)
        NodePropertyTextField(location::geoUri, state = geoUriState)

        // TODO get consent to check clipboard first
        clipboardLocation(clipboardManager)?.let { clipboardLocation ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        clearFocus()
                        location.refresh(clipboardLocation, except = listOf("zoom", "query"))
                        geoUriState.value = location.geoUri
                    }
                ) {
                    Text("Use location from clipboard:\n${clipboardManager.getText()?.text}")
                }
            }
        }

        OutlinedTextField(
            label = { Text("Latitude") },
            value = location.latitude.value,
            onValueChange = {
                location.latitude.value = it
                geoUriState.value = location.toUri().toString()
                location.refresh(except = listOf("latitude"))
            },
            enabled = inputsEnabled,
            colors = outlinedTextFieldColors(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
            keyboardActions = KeyboardActions(onDone = { clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            label = { Text("Longitude") },
            value = location.longitude.value,
            onValueChange = {
                location.longitude.value = it
                location.refresh(except = listOf("longitude"))
                geoUriState.value = location.toUri().toString()
            },
            enabled = inputsEnabled,
            colors = outlinedTextFieldColors(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
            keyboardActions = KeyboardActions(onDone = { clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            label = { Text("Zoom") },
            value = location.zoom.value,
            onValueChange = {
                location.zoom.value = it
                location.refresh(except = listOf("zoom"))
                geoUriState.value = location.toUri().toString()
            },
            enabled = inputsEnabled,
            colors = outlinedTextFieldColors(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
            keyboardActions = KeyboardActions(onDone = { clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            label = { Text("Query") },
            value = location.query.value,
            onValueChange = {
                location.query.value = it
                location.refresh(except = listOf("query"))
                geoUriState.value = location.toUri().toString()
            },
            enabled = inputsEnabled,
            colors = outlinedTextFieldColors(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
            keyboardActions = KeyboardActions(onDone = { clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
