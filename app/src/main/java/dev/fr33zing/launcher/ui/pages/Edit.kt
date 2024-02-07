package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.viewmodel.EditViewModel
import dev.fr33zing.launcher.ui.components.form.CancelButton
import dev.fr33zing.launcher.ui.components.form.FinishButton
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.components.form.payload.EditForm
import dev.fr33zing.launcher.ui.theme.Catppuccin

data class EditFormArguments(val padding: PaddingValues, val node: Node, val payload: Payload)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Edit(
    navigateBack: () -> Unit,
    viewModel: EditViewModel = hiltViewModel(),
) {
    val nodePayload by viewModel.flow.collectAsStateWithLifecycle()

    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    val preferences = Preferences(LocalContext.current)
    val askOnAccept by preferences.confirmationDialogs.editNode.askOnAccept.state
    val askOnReject by preferences.confirmationDialogs.editNode.askOnReject.state

    fun commitChanges() {
        viewModel.commitChanges(navigateBack)
        navigateBack()
    }

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel changes",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue editing",
        noIcon = Icons.Filled.ArrowBack,
        backAction = YesNoDialogBackAction.Yes,
        onYes = navigateBack,
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Save changes",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue editing",
        noIcon = Icons.Filled.ArrowBack,
        onYes = ::commitChanges,
    )

    BackHandler { if (askOnReject) cancelDialogVisible.value = true else navigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            nodePayload?.node?.let { node ->
                                append("Editing ")
                                withStyle(SpanStyle(color = node.kind.color)) {
                                    append(node.kind.label)
                                }
                            }
                        }
                    )
                },
                actions = {
                    CancelButton {
                        if (askOnReject) cancelDialogVisible.value = true else navigateBack()
                    }
                    FinishButton {
                        if (askOnAccept) saveDialogVisible.value = true else commitChanges()
                    }
                },
            )
        }
    ) { padding ->
        nodePayload?.let { (node, payload) -> EditForm(EditFormArguments(padding, node, payload)) }
    }
}
