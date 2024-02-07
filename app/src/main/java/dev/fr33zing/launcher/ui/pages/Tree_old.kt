package dev.fr33zing.launcher.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavController
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.components.tree.NodeSearchContainer
import dev.fr33zing.launcher.ui.components.tree.old.RecursiveNodeListSetup

@Composable
fun Tree_old(db: AppDatabase, navController: NavController, rootNodeId: Int?) {
    val density = LocalDensity.current
    val statusBarsTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarsBottom =
        with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val verticalPadding =
        remember(WindowInsets.statusBars, WindowInsets.navigationBars) {
            listOf(statusBarsTop, navigationBarsBottom).max()
        }
    val hiddenRatio = 0.666f
    val shadowRatio = 1f - hiddenRatio
    val hiddenHeight = verticalPadding * hiddenRatio
    val shadowHeight = verticalPadding * shadowRatio

    NodeSearchContainer(
        db,
        containerVerticalPadding = hiddenHeight,
        panelVerticalPadding = shadowHeight,
        shadowHeight = shadowHeight,
    ) { scrollState ->
        RecursiveNodeListSetup(db, navController, rootNodeId, scrollState, shadowHeight)
    }
}
