package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.checkPermission
import dev.fr33zing.launcher.data.persistent.moveNode
import dev.fr33zing.launcher.ui.components.NodePath
import dev.fr33zing.launcher.ui.components.NodePicker
import dev.fr33zing.launcher.ui.components.OutlinedValue
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.theme.Catppuccin
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val extraPadding = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Move(db: AppDatabase, navController: NavController, nodeId: Int) {
    var initialRootNodeId by remember { mutableStateOf<Int?>(null) }
    val selectedNode = remember { mutableStateOf<Node?>(null) }
    var movingNode by remember { mutableStateOf<Node?>(null) }
    var movingNodeCurrentParent by remember { mutableStateOf<Node?>(null) }

    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    LaunchedEffect(nodeId) {
        movingNode = db.nodeDao().getNodeById(nodeId) ?: throw Exception("node is null")
        val parentId = movingNode!!.parentId ?: throw Exception("parentId is null")
        movingNodeCurrentParent =
            db.nodeDao().getNodeById(parentId) ?: throw Exception("parent is null")
        initialRootNodeId = movingNodeCurrentParent!!.parentId ?: ROOT_NODE_ID
        selectedNode.value = db.nodeDao().getNodeById(initialRootNodeId!!)
    }

    YesNoDialog(
        visible = cancelDialogVisible,
        icon = Icons.Filled.Close,
        yesText = "Cancel move",
        yesColor = Catppuccin.Current.red,
        yesIcon = Icons.Filled.Close,
        noText = "Continue browsing",
        noIcon = Icons.Filled.ArrowBack,
        backAction = YesNoDialogBackAction.Yes,
        onYes = { onCancelChanges(navController) },
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Confirm move",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue browsing",
        noIcon = Icons.Filled.ArrowBack,
        onYes = { onSaveChanges(navController, db, movingNode!!, selectedNode.value?.nodeId) },
    )

    BackHandler(enabled = selectedNode.value?.nodeId == ROOT_NODE_ID) {
        cancelDialogVisible.value = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            append("Moving ")
                            if (movingNode != null) {
                                withStyle(SpanStyle(color = movingNode!!.kind.color)) {
                                    append(movingNode!!.kind.label)
                                }
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
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(extraPadding)) {
            if (movingNode != null && initialRootNodeId != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Preferences.spacingDefault),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    OutlinedValue(
                        label = "${movingNode!!.kind.label} to move",
                        modifier = Modifier.fillMaxWidth()
                    ) { padding ->
                        NodePath(db, movingNode!!, modifier = Modifier.padding(padding))
                    }

                    OutlinedValue(label = "Destination", modifier = Modifier.fillMaxWidth()) {
                        padding ->
                        if (selectedNode.value != null)
                            NodePath(db, selectedNode.value!!, modifier = Modifier.padding(padding))
                    }

                    NodePicker(
                        db,
                        initialRootNodeId,
                        selectedNode,
                        nodeVisiblePredicate = {
                            it.kind == NodeKind.Directory && it != movingNode
                        },
                        nodeBlockedReason = { node ->
                            if (node == movingNodeCurrentParent) {
                                val kind =
                                    movingNode!!.kind.label.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                        else it.toString()
                                    }
                                "$kind '${movingNode!!.label}' is already in directory '${node.label}'."
                            } else if (
                                !(db.checkPermission(
                                    PermissionKind.Move,
                                    PermissionScope.Recursive,
                                    node
                                ) ||
                                    db.checkPermission(
                                        PermissionKind.MoveIn,
                                        PermissionScope.Recursive,
                                        node
                                    ))
                            ) {
                                val kind = movingNode!!.kind.label.lowercase()
                                "Cannot move $kind '${movingNode!!.label}' into directory '${node.label}'"
                            } else {
                                null
                            }
                        }
                    )
                }
            }
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
    newParentNodeId: Int?,
) {
    CoroutineScope(Dispatchers.Main).launch {
        db.moveNode(node, newParentNodeId)
        navController.popBackStack()
    }
}
