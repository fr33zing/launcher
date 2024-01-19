package dev.fr33zing.launcher

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Move
import dev.fr33zing.launcher.ui.pages.Preferences
import dev.fr33zing.launcher.ui.pages.Reorder
import dev.fr33zing.launcher.ui.pages.Tree
import dev.fr33zing.launcher.ui.pages.Tree_old
import io.reactivex.rxjava3.subjects.PublishSubject

val GoHomeSubject = PublishSubject.create<Unit>()
var goHomeOnNextPause = true

fun doNotGoHomeOnNextPause() {
    goHomeOnNextPause = false
}

object Routes {
    object Main {
        fun default() = home()

        fun home() = "home"

        fun settings() = "settings"

        fun tree(nodeId: Int? = null) = "${home()}/tree/${nodeId ?: "{nodeId}"}"

        fun create(nodeId: Int? = null) = "create/${nodeId ?: "{nodeId}"}"

        fun reorder(nodeId: Int? = null) = "reorder/${nodeId ?: "{nodeId}"}"

        fun move(nodeId: Int? = null) = "move/${nodeId ?: "{nodeId}"}"

        fun editForm(nodeId: Int? = null) = "edit/${nodeId ?: "{nodeId}"}"
    }
}

@Composable
fun SetupNavigation(db: AppDatabase) {
    val navController = rememberNavController()

    DisposableEffect(Unit) {
        val subscription =
            GoHomeSubject.subscribe {
                if (navController.currentDestination?.route != "home")
                    navController.navigate("home")
            }
        onDispose { subscription.dispose() }
    }

    NavHost(
        navController = navController,
        graph = remember { createNavGraph(navController, db) },
        contentAlignment = Alignment.TopCenter,
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() },
    )
}

private fun createNavGraph(navController: NavController, db: AppDatabase) =
    navController.createGraph(startDestination = Routes.Main.default()) {
        val navigateBack: () -> Unit = { navController.popBackStack() }

        composable(Routes.Main.settings()) { Preferences(db) }

        composable(
            Routes.Main.home(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            Home(navController)
        }

        composable(
            Routes.Main.tree(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            val preferences = Preferences(LocalContext.current)
            val useNewTree by preferences.debug.useNewTree.state
            if (useNewTree) Tree(navigateBack) else Tree_old(db, navController, null)
        }

        composable(Routes.Main.create()) { Create(navigateBack) }

        composable(Routes.Main.reorder()) { backStackEntry ->
            Reorder(db, navController, backStackEntry.nodeId())
        }

        composable(Routes.Main.move()) { Move(navigateBack) }

        composable(Routes.Main.editForm()) { Edit(navigateBack) }
    }

// TODO move this
fun SavedStateHandle.nodeId(): Int =
    get<String>("nodeId")?.toInt() ?: throw Exception("nodeId is null")

// TODO delete these
private fun NavBackStackEntry.hasTreeRoute() = destination.route?.startsWith("home/tree/") == true

private fun NavBackStackEntry.nodeIdOrNull() = arguments?.getString("nodeId")?.toInt()

private fun NavBackStackEntry.nodeId() = nodeIdOrNull() ?: throw Exception("nodeId is null")

private fun NavBackStackEntry.nodeKind() =
    arguments?.getString("nodeKind") ?: throw Exception("nodeKind is null")
