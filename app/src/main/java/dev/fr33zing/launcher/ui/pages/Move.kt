package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.MoveViewModel
import dev.fr33zing.launcher.data.viewmodel.sendJumpToNode
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialog
import dev.fr33zing.launcher.ui.components.dialog.YesNoDialogBackAction
import dev.fr33zing.launcher.ui.components.form.CancelButton
import dev.fr33zing.launcher.ui.components.form.FinishButton
import dev.fr33zing.launcher.ui.components.form.NodePath
import dev.fr33zing.launcher.ui.components.form.OutlinedValue
import dev.fr33zing.launcher.ui.components.tree.TreeBrowser
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.verticalScrollShadows

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Move(
    navigateBack: () -> Unit,
    viewModel: MoveViewModel = hiltViewModel(),
) {
    val treeBrowserState by viewModel.treeBrowser.flow.collectAsStateWithLifecycle()
    val selectedNode by remember { derivedStateOf { treeBrowserState?.stack?.last() } }

    val cancelDialogVisible = remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    val preferences = Preferences(LocalContext.current)
    val spacing by preferences.nodeAppearance.spacing.state
    val askOnAccept by preferences.confirmationDialogs.moveNode.askOnAccept.state
    val askOnReject by preferences.confirmationDialogs.moveNode.askOnReject.state

    fun jumpToNode() {
        sendJumpToNode(viewModel.nodeToMove?.nodeId ?: throw Exception("nodeToMove is null"))
    }

    fun cancelMove() {
        navigateBack()
    }

    fun commitMove() {
        viewModel.commitMove()
        navigateBack()
        jumpToNode()
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
        onYes = ::cancelMove,
    )

    YesNoDialog(
        visible = saveDialogVisible,
        icon = Icons.Filled.Check,
        yesText = "Confirm move",
        yesColor = Catppuccin.Current.green,
        yesIcon = Icons.Filled.Check,
        noText = "Continue browsing",
        noIcon = Icons.Filled.ArrowBack,
        onYes = ::commitMove,
    )

    BackHandler(enabled = selectedNode?.nodeId == ROOT_NODE_ID) {
        if (askOnReject) cancelDialogVisible.value = true else cancelMove()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            append("Moving ")
                            if (viewModel.nodeToMove != null) {
                                withStyle(
                                    SpanStyle(color = viewModel.nodeToMove.notNull().kind.color)
                                ) {
                                    append(viewModel.nodeToMove!!.kind.label)
                                }
                            }
                        }
                    )
                },
                actions = {
                    CancelButton {
                        if (askOnReject) cancelDialogVisible.value = true else cancelMove()
                    }
                    FinishButton {
                        if (askOnAccept) saveDialogVisible.value = true else commitMove()
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val topPadding = remember(innerPadding) { innerPadding.calculateTopPadding() }
            val bottomPadding = remember(innerPadding) { innerPadding.calculateBottomPadding() }

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(preferences.nodeAppearance.spacing.mappedDefault),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.absolutePadding(top = topPadding, bottom = bottomPadding)
            ) {
                val valueModifier = remember {
                    Modifier.padding(horizontal = ScreenHorizontalPadding).fillMaxWidth()
                }

                OutlinedValue(label = "Current path", modifier = valueModifier) { padding ->
                    NodePath(viewModel.nodeToMoveLineage, modifier = Modifier.padding(padding))
                }

                OutlinedValue(label = "Destination path", modifier = valueModifier) { padding ->
                    if (selectedNode != null && treeBrowserState != null)
                        NodePath(treeBrowserState!!.stack, modifier = Modifier.padding(padding))
                }

                val verticalPadding = spacing / 2
                Box(Modifier.fillMaxSize().verticalScrollShadows(verticalPadding)) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = verticalPadding)
                    ) {
                        TreeBrowser(
                            viewModel.treeBrowser,
                            horizontalPadding = ScreenHorizontalPadding
                        )
                    }
                }
            }
        }
    }
}
