package dev.fr33zing.launcher

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Move
import dev.fr33zing.launcher.ui.pages.Preferences
import dev.fr33zing.launcher.ui.pages.Reorder
import dev.fr33zing.launcher.ui.pages.Search
import dev.fr33zing.launcher.ui.pages.Setup
import dev.fr33zing.launcher.ui.pages.Tree
import dev.fr33zing.launcher.ui.pages.Tree_old
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.math.roundToInt

val GoHomeSubject = PublishSubject.create<Unit>()
var goHomeOnNextPause = true

fun doNotGoHomeOnNextPause() {
    goHomeOnNextPause = false
}

object Routes {
    object Main {
        fun default() = home()

        fun home() = "home"

        fun setup() = "setup"

        fun settings() = "settings"

        fun search() = "search"

        fun tree(nodeId: Int? = null) = "${home()}/tree/${nodeId ?: "{nodeId}"}"

        fun create(nodeId: Int? = null) = "create/${nodeId ?: "{nodeId}"}"

        fun reorder(nodeId: Int? = null) = "reorder/${nodeId ?: "{nodeId}"}"

        fun move(nodeId: Int? = null) = "move/${nodeId ?: "{nodeId}"}"

        fun edit(nodeId: Int? = null) = "edit/${nodeId ?: "{nodeId}"}"
    }
}

class TreeNavigator(navController: NavController) {
    val search = { navController.navigate(Routes.Main.search()) }
    val create = { nodeId: Int -> navController.navigate(Routes.Main.create(nodeId)) }
    val reorder = { nodeId: Int -> navController.navigate(Routes.Main.reorder(nodeId)) }
    val move = { nodeId: Int -> navController.navigate(Routes.Main.move(nodeId)) }
    val edit = { nodeId: Int -> navController.navigate(Routes.Main.edit(nodeId)) }
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
        val navigateTo: (String) -> (() -> Unit) = { { navController.navigate(it) } }

        composable(Routes.Main.settings()) { Preferences(db) }

        composable(Routes.Main.setup()) { Setup(navigateToHome = navigateTo(Routes.Main.home())) }

        composable(
            Routes.Main.home(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            Home(
                navigateToSetup = navigateTo(Routes.Main.setup()),
                navigateToTree = navigateTo(Routes.Main.tree(ROOT_NODE_ID)),
                navigateToSettings = navigateTo(Routes.Main.settings()),
                navigateToSearch = navigateTo(Routes.Main.search()),
            )
        }

        val searchOffsetFactor = 1f / 2
        val searchOffsetY = { it: Int -> (-it * searchOffsetFactor).roundToInt() }
        val searchAnimSpec: FiniteAnimationSpec<IntOffset> =
            spring(
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        composable(
            Routes.Main.search(),
            enterTransition = { fadeIn() + slideInVertically(searchAnimSpec, searchOffsetY) },
            exitTransition = { fadeOut() + slideOutVertically(searchAnimSpec, searchOffsetY) },
            popEnterTransition = { fadeIn() + slideInVertically(searchAnimSpec, searchOffsetY) },
            popExitTransition = { fadeOut() + slideOutVertically(searchAnimSpec, searchOffsetY) },
        ) {
            Search()
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
            if (useNewTree) Tree(navigateBack, remember { TreeNavigator(navController) })
            else Tree_old(db, navController, null)
        }

        composable(Routes.Main.create()) { Create(navigateBack) }

        composable(Routes.Main.reorder()) { Reorder(navigateBack) }

        composable(Routes.Main.move()) { Move(navigateBack) }

        composable(Routes.Main.edit()) { Edit(navigateBack) }
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
