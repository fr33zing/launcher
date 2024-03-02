package dev.fr33zing.launcher.ui.components.form.payload

import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.fr33zing.launcher.data.persistent.payloads.File
import dev.fr33zing.launcher.data.utility.queryContentUriActivities
import dev.fr33zing.launcher.data.utility.toLauncherActivityInfos
import dev.fr33zing.launcher.doNotGoHomeOnNextPause
import dev.fr33zing.launcher.ui.components.GiantPickerButton
import dev.fr33zing.launcher.ui.components.GiantPickerButtonContainer
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.dialog.ApplicationPickerDialog
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.components.form.refreshNodePropertyTextFields
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.pages.EditFormArguments
import dev.fr33zing.launcher.ui.theme.Dim
import dev.fr33zing.launcher.ui.theme.typography

private fun uriLabel(uri: Uri) =
    uri.pathSegments.lastOrNull()?.split(java.io.File.separatorChar)?.last() ?: ""

private fun uriFilePath(uri: Uri) = uri.toString()

@Composable
fun FileEditForm(arguments: EditFormArguments) {
    val context = LocalContext.current
    val (padding, node, payload) = arguments
    val file = payload as File

    var defaultLabel by remember { mutableStateOf<String?>(null) }
    var contentUriActivities by remember { mutableStateOf<List<LauncherActivityInfo>>(emptyList()) }

    val labelState = remember { mutableStateOf(node.label) }
    val filePathState = remember { mutableStateOf(file.filePath) }
    val openWithPackageNameState = remember { mutableStateOf(file.openWithPackageName) }

    fun onUriChanged() {
        val uri = Uri.parse(filePathState.value)
        defaultLabel = uriLabel(uri)
        contentUriActivities =
            context.queryContentUriActivities(uri).toLauncherActivityInfos(context)
    }
    LaunchedEffect(Unit) { onUriChanged() }

    val applicationPickerVisible = remember { mutableStateOf(false) }
    ApplicationPickerDialog(
        applicationPickerVisible,
        onAppPicked = {
            openWithPackageNameState.value = it.applicationInfo.packageName
            file.openWithPackageName = openWithPackageNameState.value
            refreshNodePropertyTextFields()
        },
        overrideActivityInfos = contentUriActivities
    )

    EditFormColumn(padding) {
        GiantPickerButtonContainer {
            GiantPickerButton(
                text = "Pick file",
                onPicked = { uri ->
                    // TODO remove unused permissions
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    labelState.value = uriLabel(uri)
                    node.label = labelState.value
                    filePathState.value = uriFilePath(uri)
                    file.filePath = filePathState.value
                    refreshNodePropertyTextFields()
                    onUriChanged()
                },
                dialog = { dialogVisible, onPicked ->
                    val openDocumentLauncher =
                        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                            uri ->
                            uri?.let { onPicked(uri) }
                                ?: sendNotice( // TODO revise wording, maybe delete altogether
                                    "file-edit-form-picker-failed-uri-null",
                                    "File picker failed: No file path was provided.",
                                    NoticeKind.Error
                                )
                            dialogVisible.value = false
                        }

                    LaunchedEffect(dialogVisible.value) {
                        if (!dialogVisible.value) return@LaunchedEffect

                        doNotGoHomeOnNextPause()
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    }
                }
            )
        }

        // TODO set default label value
        NodePropertyTextField(property = node::label, state = labelState, userCanRevert = true)
        NodePropertyTextField(file::filePath, filePathState)
        NodePropertyTextField(file::openWithPackageName, openWithPackageNameState)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()
        ) {
            if (filePathState.value.isEmpty())
                Text(
                    "No file selected.",
                    style = typography.labelMedium,
                    color = Dim,
                )
            else
                Text(
                    "${contentUriActivities.size} applications can open this file.",
                    style = typography.labelMedium,
                )
            Button(
                onClick = {
                    onUriChanged()
                    applicationPickerVisible.value = true
                }
            ) {
                Text("Pick app")
            }
        }
    }
}
