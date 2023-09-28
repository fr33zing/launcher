package com.example.mylauncher.ui.pages

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
import com.example.mylauncher.data.AppDatabase
import com.example.mylauncher.data.Node
import com.example.mylauncher.ui.components.dialog.YesNoDialog
import com.example.mylauncher.ui.theme.Catppuccin
import com.example.mylauncher.ui.util.getUserEditableAnnotation
import kotlin.reflect.KMutableProperty0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Edit(db: AppDatabase, navController: NavController, nodeId: Int) {
    var node by remember { mutableStateOf<Node?>(null) }
    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        node = db.nodeDao()
            .getNodeById(nodeId)
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
            onYes = { navController.popBackStack() },
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
            onYes = { navController.popBackStack() },
        )

        Scaffold(topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(),
                title = {
                    Text(text = buildAnnotatedString {
                        append("Editing ")
                        withStyle(SpanStyle(color = node!!.kind.color)) {
                            append(node!!.kind.label)
                        }
                    })
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
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EditForm(node!!)
            }
        }
    }
}

@Composable
private fun EditForm(node: Node) {
    NodePropertyTextField(node::label)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun NodePropertyTextField(
    property: KMutableProperty0<String>,
    imeAction: ImeAction = ImeAction.Done,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val annotation = remember { property.getUserEditableAnnotation() }
    var state by remember { mutableStateOf(property.get()) }

    OutlinedTextField(
        value = state,
        onValueChange = { state = it; property.set(it) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            imeAction = imeAction,
            keyboardType = KeyboardType.Text,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        ),
        label = { Text(annotation.label) },
        supportingText = if (annotation.supportingText.isNotEmpty()) {
            { Text(annotation.supportingText) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
    )
}