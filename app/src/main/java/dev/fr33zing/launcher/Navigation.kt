package dev.fr33zing.launcher

import android.os.Bundle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Move
import dev.fr33zing.launcher.ui.pages.Preferences
import dev.fr33zing.launcher.ui.pages.Reorder
import dev.fr33zing.launcher.ui.pages.Tree
import io.reactivex.rxjava3.subjects.PublishSubject

val GoHomeSubject = PublishSubject.create<Unit>()
var goHomeOnNextPause = true

fun doNotGoHomeOnNextPause() {
    goHomeOnNextPause = false
}

class NavigationService() : NavController.OnDestinationChangedListener {
    lateinit var destination: NavDestination
    private var arguments: Bundle? = null

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        this.destination = destination
        this.arguments = arguments
    }

    fun hasTreeRoute() = destination.route?.startsWith("home/tree/") == true

    fun nodeIdOrNull() = arguments?.getString("nodeId")?.toInt()

    fun nodeId() = nodeIdOrNull() ?: throw Exception("nodeId is null")
}

@Composable
fun SetupNavigation(db: AppDatabase, navService: NavigationService) {
    val navController = rememberNavController()
    navController.addOnDestinationChangedListener(navService)

    DisposableEffect(Unit) {
        val subscription =
            GoHomeSubject.subscribe {
                if (navController.currentDestination?.route != "home")
                    navController.navigate("home")
            }
        onDispose { subscription.dispose() }
    }

    NavHost(
        navController,
        startDestination = "home",
        enterTransition = {
            if (initialState.hasTreeRoute()) fadeIn()
            else if (targetState.hasTreeRoute()) slideInVertically { it } + fadeIn()
            else slideInHorizontally { it } + fadeIn()
        },
        exitTransition = {
            if (initialState.hasTreeRoute()) slideOutVertically { it } + fadeOut()
            else if (targetState.hasTreeRoute()) fadeOut()
            else slideOutHorizontally { -it } + fadeOut()
        },
        popEnterTransition = {
            if (initialState.hasTreeRoute()) fadeIn() else slideInHorizontally { -it } + fadeIn()
        },
        popExitTransition = {
            if (initialState.hasTreeRoute()) slideOutVertically { it } + fadeOut()
            else slideOutHorizontally { it } + fadeOut()
        },
    ) {
        composable("settings") { Preferences(db) }
        composable("home") { Home(navController) }
        composable("home/tree/{nodeId}") { backStackEntry ->
            Tree(db, navController, backStackEntry.nodeIdOrNull())
        }
        composable("edit/{nodeId}") { backStackEntry ->
            Edit(db, navController, backStackEntry.nodeId())
        }
        composable("create/{nodeId}") { backStackEntry ->
            Create(db, navController, backStackEntry.nodeId())
        }
        composable("reorder/{nodeId}") { backStackEntry ->
            Reorder(db, navController, backStackEntry.nodeId())
        }
        composable("move/{nodeId}") { Move(cancelMove = { navController.popBackStack() }) }
    }
}

private fun NavBackStackEntry.hasTreeRoute() = destination.route?.startsWith("home/tree/") == true

private fun NavBackStackEntry.nodeIdOrNull() = arguments?.getString("nodeId")?.toInt()

private fun NavBackStackEntry.nodeId() = nodeIdOrNull() ?: throw Exception("nodeId is null")
