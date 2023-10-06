package com.example.mylauncher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavController
import androidx.room.withTransaction
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.data.persistent.Node
import com.example.mylauncher.data.persistent.payloads.Payload
import com.example.mylauncher.ui.components.EditForm
import com.example.mylauncher.ui.components.dialog.YesNoDialog
import com.example.mylauncher.ui.components.dialog.YesNoDialogBackAction
import com.example.mylauncher.ui.components.refreshNodeList
import com.example.mylauncher.ui.theme.Catppuccin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            payload = if (node != null) db.getPayloadByNodeId(node!!.kind, node!!.nodeId) else null
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
            backAction = YesNoDialogBackAction.Yes,
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
            onYes = { onSaveChanges(navController, db, node!!, payload) },
        )

        BackHandler { cancelDialogVisible.value = true }

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
            EditForm(innerPadding, node!!, payload)
        }
    }
}

private fun onCancelChanges(navController: NavController) {
    navController.popBackStack()
}

private fun onSaveChanges(
    navController: NavController,
    db: AppDatabase,
    node: Node,
    payload: Payload?
) {
    CoroutineScope(Dispatchers.Main).launch {
        db.withTransaction {
            db.update(node)
            payload?.let { db.update(it) }
        }

        refreshNodeList()
        navController.popBackStack()
    }
}
