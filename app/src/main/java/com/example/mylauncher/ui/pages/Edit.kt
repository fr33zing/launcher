package com.example.mylauncher.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.room.withTransaction
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.data.persistent.Application
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.Payload
import com.example.mylauncher.helper.conditional
import com.example.mylauncher.helper.longPressable
import com.example.mylauncher.ui.components.dialog.YesNoDialog
import com.example.mylauncher.ui.theme.Catppuccin
import com.example.mylauncher.ui.theme.DisabledTextFieldColor
import com.example.mylauncher.ui.theme.Foreground
import com.example.mylauncher.ui.theme.outlinedTextFieldColors
import com.example.mylauncher.ui.util.getUserEditableAnnotation
import kotlin.reflect.KMutableProperty0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Edit(db: AppDatabase, navController: NavController, nodeId: Int) {
    var node by remember { mutableStateOf<Node?>(null) }
    var payload by remember { mutableStateOf<Payload?>(null) }
    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            node = db.nodeDao().getNodeById(nodeId)
            payload =
                if (node != null) db.payloadDao(node!!.kind)?.getByNodeId(node!!.nodeId) else null

            if (node != null && payload != null && payload!!::class != node!!.kind.payloadClass!!)
                throw Exception(
                    "Mismatched payload class for node kind.\nNode: $node\nPayload: ${payload!!::class}"
                )
        }
    }

    if (node == null) {
        Text(text = "Node does not exist!")
    } else {
        YesNoDialog(
            visible = cancelDialogVisible,
            icon = Icons.Filled.Close,
            yesText = "Cancel changes",
            yesColor = Catppuccin.Current.red,
            yesIcon = Icons.Filled.Close,
            noText = "Continue editing",
            noColor = Color(0xFF888888),
            noIcon = Icons.Filled.ArrowBack,
            onYes = { onCancelChanges(navController) },
        )

        YesNoDialog(
            visible = saveDialogVisible,
            icon = Icons.Filled.Check,
            yesText = "Save changes",
            yesColor = Catppuccin.Current.green,
            yesIcon = Icons.Filled.Check,
            noText = "Continue editing",
            noColor = Color(0xFF888888),
            noIcon = Icons.Filled.ArrowBack,
            onYes = { onSaveChanges(navController, db, node!!) },
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(),
                    title = {
                        Text(
                            buildAnnotatedString {
                                append("Editing ")
                                withStyle(SpanStyle(color = node!!.kind.color)) {
                                    append(node!!.kind.label)
                                }
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { cancelDialogVisible.value = true }) {
                            Icon(Icons.Filled.Close, "cancel", tint = Catppuccin.Current.red)
                        }
                        IconButton(onClick = { saveDialogVisible.value = true }) {
                            Icon(Icons.Filled.Check, "finish", tint = Catppuccin.Current.green)
                        }
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EditForm(node!!, payload)
            }
        }
    }
}

private fun onCancelChanges(navController: NavController) {
    navController.popBackStack()
}

private fun onSaveChanges(navController: NavController, db: AppDatabase, node: Node) {
    CoroutineScope(Dispatchers.Main).launch {
        db.withTransaction {
            db.nodeDao().update(node)

            // TODO update kind-specific data
        }

        navController.popBackStack()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun NodePropertyTextField(
    property: KMutableProperty0<String>,
    defaultValue: String? = null,
    userCanRevert: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val annotation = remember { property.getUserEditableAnnotation() }
    val input = remember { mutableStateOf(property.get()) }
    val initialValue = remember { defaultValue ?: input.value }
    val locked = remember { mutableStateOf(annotation.locked) }
    var enabled by remember { mutableStateOf(true) }

    fun setValue(value: String) {
        input.value = value
        property.set(value)
    }

    fun clearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()

        // HACK: Quickly disable and enable to clear selection
        enabled = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(25)
            enabled = true
        }
    }

    OutlinedTextField(
        value = input.value,
        colors = outlinedTextFieldColors(),
        readOnly = locked.value,
        enabled = !locked.value && enabled,
        onValueChange = ::setValue,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = imeAction,
                keyboardType = KeyboardType.Text,
            ),
        keyboardActions = KeyboardActions(onDone = { clearFocus() }),
        label = { Text(annotation.label) },
        supportingText =
            if (annotation.supportingText.isNotEmpty()) {
                { Text(annotation.supportingText) }
            } else null,
        trailingIcon = {
            AnimatedContent(
                targetState = userCanRevert && input.value != initialValue,
                label = "node property text field: lock/revert button"
            ) { showRevertButton ->
                if (showRevertButton) {
                    RevertButton {
                        locked.value = annotation.locked
                        setValue(initialValue)
                        clearFocus()
                    }
                } else if (annotation.locked) {
                    ToggleLockedButton(locked, annotation.userCanUnlock)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ToggleLockedButton(locked: MutableState<Boolean>, userCanUnlock: Boolean) {
    Icon(
        if (locked.value) Icons.Filled.Lock else Icons.Filled.LockOpen,
        contentDescription = if (locked.value) "closed lock" else "open lock",
        modifier =
            Modifier.conditional(userCanUnlock) {
                longPressable() { locked.value = !locked.value }
            },
        tint = if (userCanUnlock) Foreground else DisabledTextFieldColor
    )
}

@Composable
private fun RevertButton(onLongPressed: () -> Unit) {
    Icon(
        Icons.Filled.Undo,
        contentDescription = "revert changes",
        modifier = Modifier.longPressable(onLongPressed)
    )
}

@Composable
private fun EditForm(node: Node, payload: Payload?) {
    if (payload != null)
        when (node.kind) {
            NodeKind.Application -> ApplicationEditForm(payload, node)
            else -> DefaultEditForm(node)
        }
    else DefaultEditForm(node)
}

@Composable
private fun DefaultEditForm(node: Node) {
    NodePropertyTextField(node::label)
}

@Composable
private fun ApplicationEditForm(
    payload: Payload?,
    node: Node,
) {
    val application = payload as Application
    NodePropertyTextField(node::label, defaultValue = application.appName, userCanRevert = true)
    NodePropertyTextField(application::appName)
    NodePropertyTextField(application::packageName)
    NodePropertyTextField(application::activityClassName, userCanRevert = true)
    NodePropertyTextField(application::userHandle)
}
