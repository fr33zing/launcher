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
import dev.fr33zing.launcher.ui.pages.ViewNote
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.math.roundToInt

val GoHomeSubject = PublishSubject.create<Unit>()
var goHomeOnNextPause = true

fun doNotGoHomeOnNextPause() {
    goHomeOnNextPause = false
}

object Routes {
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

    fun viewNote(nodeId: Int? = null) = "viewNote/${nodeId ?: "{nodeId}"}"
}

private fun NavController.navigateOnce(route: String) = navigate(route) { launchSingleTop = true }

class TreeNavigator(navController: NavController) {
    val search = { navController.navigateOnce(Routes.search()) }
    val create = { nodeId: Int -> navController.navigateOnce(Routes.create(nodeId)) }
    val reorder = { nodeId: Int -> navController.navigateOnce(Routes.reorder(nodeId)) }
    val move = { nodeId: Int -> navController.navigateOnce(Routes.move(nodeId)) }
    val edit = { nodeId: Int -> navController.navigateOnce(Routes.edit(nodeId)) }
    val viewNote = { nodeId: Int -> navController.navigateOnce(Routes.viewNote(nodeId)) }
}

@Composable
fun SetupNavigation(db: AppDatabase) {
    val navController = rememberNavController()

    DisposableEffect(Unit) {
        val subscription =
            GoHomeSubject.subscribe {
                if (navController.currentDestination?.route != "home")
                    navController.navigateOnce("home")
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
    navController.createGraph(startDestination = Routes.default()) {
        val navigateBack: () -> Unit = { navController.popBackStack() }
        val navigateTo: (String) -> (() -> Unit) = { { navController.navigateOnce(it) } }

        composable(Routes.settings()) { Preferences(db) }

        composable(Routes.setup()) { Setup(navigateToHome = navigateTo(Routes.home())) }

        composable(
            Routes.home(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            Home(
                navigateToSetup = navigateTo(Routes.setup()),
                navigateToTree = navigateTo(Routes.tree(ROOT_NODE_ID)),
                navigateToSettings = navigateTo(Routes.settings()),
                navigateToSearch = navigateTo(Routes.search()),
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
            Routes.search(),
            enterTransition = { fadeIn() + slideInVertically(searchAnimSpec, searchOffsetY) },
            exitTransition = { fadeOut() + slideOutVertically(searchAnimSpec, searchOffsetY) },
            popEnterTransition = { fadeIn() + slideInVertically(searchAnimSpec, searchOffsetY) },
            popExitTransition = { fadeOut() + slideOutVertically(searchAnimSpec, searchOffsetY) },
        ) {
            Search(
                navigateBack = navigateBack,
                navigateToTree = navigateTo(Routes.tree(ROOT_NODE_ID))
            )
        }

        composable(
            Routes.tree(),
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

        composable(Routes.create()) { Create(navigateBack) }

        composable(Routes.reorder()) { Reorder(navigateBack) }

        composable(Routes.move()) { Move(navigateBack) }

        composable(Routes.edit()) { Edit(navigateBack) }

        composable(Routes.viewNote()) {
            ViewNote(
                navigateToEdit = { nodeId: Int -> navController.navigateOnce(Routes.edit(nodeId)) }
            )
        }
    }

// TODO move this
fun SavedStateHandle.nodeId(): Int =
    get<String>("nodeId")?.toInt() ?: throw Exception("nodeId is null")
