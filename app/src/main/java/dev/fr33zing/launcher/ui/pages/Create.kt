package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.viewmodel.CreateViewModel
import dev.fr33zing.launcher.data.viewmodel.sendJumpToNode
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.components.form.CancelButton
import dev.fr33zing.launcher.ui.components.form.FinishButton
import dev.fr33zing.launcher.ui.components.form.NodeEditForm
import dev.fr33zing.launcher.ui.theme.Catppuccin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Create(navigateBack: () -> Unit, viewModel: CreateViewModel = hiltViewModel()) {
    val nodePayload = viewModel.nodePayload
    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    val preferences = Preferences(LocalContext.current)
    val askOnAccept by preferences.confirmationDialogs.createNode.askOnAccept.state
    val askOnReject by preferences.confirmationDialogs.createNode.askOnReject.state

    // If non-null, the save button will be disabled and tapping it will show the value in a notice.
    var disableSavingReason by remember { mutableStateOf<String?>(null) }

    fun disableSaving(message: String) {
        disableSavingReason = message
    }

    fun enableSaving() {
        disableSavingReason = null
    }

    fun jumpToNode() {
        nodePayload?.node?.nodeId?.let {
            sendJumpToNode(nodeId = it, snap = false, afterNextUpdate = true)
        } ?: throw Exception("nodePayload is null")
    }

    fun cancelChanges() {
        viewModel.cancelChanges(navigateBack)
    }

    fun commitChanges() {
        viewModel.commitChanges {
            navigateBack()
            jumpToNode()
        }
    }

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel creation",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue editing",
        noIcon = Icons.Filled.ArrowBack,
        backAction = YesNoDialogBackAction.Yes,
        onYes = ::cancelChanges,
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Create ${nodePayload?.node?.kind?.label}",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue editing",
        noIcon = Icons.Filled.ArrowBack,
        onYes = ::commitChanges,
    )

    BackHandler { if (askOnReject) cancelDialogVisible.value = true else cancelChanges() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            nodePayload?.node?.let { node ->
                                append("Creating ")
                                withStyle(SpanStyle(color = node.kind.color)) {
                                    append(node.kind.label)
                                }
                            }
                        }
                    )
                },
                actions = {
                    CancelButton {
                        if (askOnReject) cancelDialogVisible.value = true else cancelChanges()
                    }
                    FinishButton(disableSavingReason) {
                        if (askOnAccept) saveDialogVisible.value = true else commitChanges()
                    }
                },
            )
        }
    ) { padding ->
        nodePayload?.let { (node, payload) ->
            NodeEditForm(
                EditFormArguments(padding, node, payload, true, ::disableSaving, ::enableSaving)
            )
        }
    }
}
