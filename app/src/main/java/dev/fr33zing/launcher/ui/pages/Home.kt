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
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.viewmodel.HomeViewModel
import dev.fr33zing.launcher.ui.components.Clock
import dev.fr33zing.launcher.ui.components.TreeBrowser
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.utility.detectFlingUp
import dev.fr33zing.launcher.ui.utility.longPressable

@Composable
fun Home(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current

    viewModel.treeBrowser.onNodeSelected { it.activate(context) }

    fun onFlingUp() = navController.navigate("home/tree/$ROOT_NODE_ID")

    BackHandler { /* Prevent back button loop */}

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        modifier =
            Modifier.systemBarsPadding()
                .padding(vertical = 32.dp)
                .fillMaxSize()
                .longPressable { navController.navigate("settings") }
                .pointerInput(Unit) { detectFlingUp(::onFlingUp) }
    ) {
        Clock(ScreenHorizontalPadding)
        TreeBrowser(viewModel.treeBrowser, modifier = Modifier.weight(1f))
    }
}
