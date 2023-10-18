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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavController
import androidx.room.withTransaction
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.NodeUpdatedSubject
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.ui.components.CancelButton
import dev.fr33zing.launcher.ui.components.EditForm
import dev.fr33zing.launcher.ui.components.FinishButton
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.theme.Catppuccin
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
            node = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Node does not exist")
            payload =
                db.getPayloadByNodeId(node!!.kind, node!!.nodeId)
                    ?: throw Exception("Payload does not exist")
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
            noIcon = Icons.Filled.ArrowBack,
            onYes = { onSaveChanges(navController, db, node!!, payload) },
        )

        BackHandler { cancelDialogVisible.value = true }

        Scaffold(
            topBar = {
                TopAppBar(
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
                        CancelButton { cancelDialogVisible.value = true }
                        FinishButton { saveDialogVisible.value = true }
                    },
                )
            }
        ) { innerPadding ->
            EditForm(db, innerPadding, node!!, payload!!)
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

        NodeUpdatedSubject.onNext(Pair(node.nodeId, node.parentId!!))
        navController.popBackStack()
    }
}
