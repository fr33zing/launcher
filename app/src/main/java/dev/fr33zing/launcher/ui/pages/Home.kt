package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fr33zing.launcher.data.viewmodel.HomeViewModel
import dev.fr33zing.launcher.ui.components.Clock
import dev.fr33zing.launcher.ui.components.tree.TreeBrowser
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.detectFling
import dev.fr33zing.launcher.ui.utility.longPressable

@Composable
fun Home(
    navigateToSetup: () -> Unit,
    navigateToTree: () -> Unit,
    navigateToSettings: () -> Unit,
    navigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    if (viewModel.isFirstRun) navigateToSetup()

    val context = LocalContext.current

    viewModel.treeBrowser.onNodeSelected { viewModel.activatePayload(context, it.payload) }

    BackHandler { /* Prevent back button loop */}

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier =
            Modifier.systemBarsPadding()
                .padding(vertical = 32.dp)
                .fillMaxSize()
                .longPressable { navigateToSettings() }
                .pointerInput(Unit) {
                    detectFling(onFlingUp = navigateToTree, onFlingDown = navigateToSearch)
                }
    ) {
        Clock(viewModel.nextAlarmFlow, ScreenHorizontalPadding)
        TreeBrowser(viewModel.treeBrowser, center = true, modifier = Modifier.weight(1f))
    }
}
