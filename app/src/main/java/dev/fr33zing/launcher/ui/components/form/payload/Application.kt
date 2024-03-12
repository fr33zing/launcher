package dev.fr33zing.launcher.ui.components.form.payload

import android.os.Process
import androidx.compose.runtime.Composable
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.ui.components.GiantPickerButton
import dev.fr33zing.launcher.ui.components.GiantPickerButtonContainer
import dev.fr33zing.launcher.ui.components.dialog.ApplicationPickerDialog
import dev.fr33zing.launcher.ui.components.form.EditFormColumn
import dev.fr33zing.launcher.ui.components.form.NodePropertyTextField
import dev.fr33zing.launcher.ui.components.form.refreshNodePropertyTextFields
import dev.fr33zing.launcher.ui.pages.EditFormArguments

@Composable
fun ApplicationEditForm(arguments: EditFormArguments) {
    val (padding, node, payload) = arguments
    val application = payload as Application

    EditFormColumn(padding) {
        GiantPickerButtonContainer {
            GiantPickerButton(
                text = "Pick app",
                onPicked = {
                    with(application) {
                        appName = it.label.toString()
                        packageName = it.applicationInfo.packageName
                        activityClassName = it.componentName.className
                        userHandle = it.user.toString()
                    }
                    node.label = application.appName
                    refreshNodePropertyTextFields()
                },
                dialog = { dialogVisible, onPicked -> ApplicationPickerDialog(dialogVisible, onPicked) },
            )
        }

        NodePropertyTextField(node::label, defaultValue = application.appName, userCanRevert = true)
        NodePropertyTextField(application::appName)
        NodePropertyTextField(application::packageName)
        NodePropertyTextField(application::activityClassName, userCanRevert = true)
        NodePropertyTextField(application::userHandle, defaultValue = Process.myUserHandle().toString())
    }
}
