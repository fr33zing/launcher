package dev.fr33zing.launcher.ui.components.form.payload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.fr33zing.launcher.data.persistent.payloads.File
import dev.fr33zing.launcher.doNotGoHomeOnNextPause
import dev.fr33zing.launcher.ui.components.GiantPickerButton
import dev.fr33zing.launcher.ui.components.GiantPickerButtonContainer
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.components.form.refreshNodePropertyTextFields
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import kotlin.io.path.Path
import kotlin.io.path.name

private fun basename(filePath: String) = Path(filePath).fileName.name

@Composable
fun FileEditForm(arguments: EditFormArguments) {
    val (padding, node, payload) = arguments
    val file = payload as File

    val labelState = remember { mutableStateOf(node.label) }
    val filePathState = remember { mutableStateOf(file.filePath) }

    EditFormColumn(padding) {
        GiantPickerButtonContainer {
            GiantPickerButton(
                text = "Pick file",
                onPicked = {
                    labelState.value = basename(it)
                    node.label = labelState.value
                    filePathState.value = it
                    file.filePath = filePathState.value
                    refreshNodePropertyTextFields()
                },
                dialog = { dialogVisible, onPicked ->
                    val openDocumentLauncher =
                        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                            dialogVisible.value = false
                            it?.path?.let { filePath -> onPicked(filePath) }
                                // TODO revise wording, maybe delete altogether
                                ?: sendNotice(
                                    "file-edit-form-picker-failed-uri-null",
                                    "File picker failed: No file path was provided.",
                                    NoticeKind.Error
                                )
                        }

                    LaunchedEffect(dialogVisible.value) {
                        if (!dialogVisible.value) return@LaunchedEffect

                        doNotGoHomeOnNextPause()
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    }
                }
            )
        }

        val defaultLabel = remember(labelState.value) { basename(labelState.value) }
        NodePropertyTextField(
            property = node::label,
            state = labelState,
            defaultValue = defaultLabel,
            userCanRevert = true
        )
        NodePropertyTextField(file::filePath, filePathState)
        NodePropertyTextField(file::openWithPackageName)
    }
}
