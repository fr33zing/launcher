package dev.fr33zing.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.room.withTransaction
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.autoCategorizeNewApplications
import dev.fr33zing.launcher.data.persistent.createNewApplications
import dev.fr33zing.launcher.data.persistent.payloads.launcherApps
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import dev.fr33zing.launcher.data.persistent.payloads.userManager
import dev.fr33zing.launcher.helper.getActivityInfos
import dev.fr33zing.launcher.ui.components.Notices
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Move
import dev.fr33zing.launcher.ui.pages.Reorder
import dev.fr33zing.launcher.ui.pages.Tree
import dev.fr33zing.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val TAG = "dev.fr33zing.launcher"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainPackageManager = packageManager
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        userManager = getSystemService(Context.USER_SERVICE) as UserManager

        window.setDecorFitsSystemWindows(false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val db =
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()

        setContent {
            LauncherTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box {
                        Main(db)
                        Notices()
                    }

                    // Check for new apps
                    LaunchedEffect(Unit) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val isFirstRun = db.nodeDao().getNodeById(ROOT_NODE_ID) == null
                            val activityInfos = getActivityInfos(applicationContext)
                            db.withTransaction {
                                db.createNewApplications(activityInfos)
                                if (isFirstRun) db.autoCategorizeNewApplications(applicationContext)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Main(db: AppDatabase) {
        // TODO investigate bug that causes tree to slide horizontally instead of vertically
        val navController = rememberNavController()
        NavHost(
            navController,
            startDestination = "home",
            enterTransition = {
                if (targetState.destination.hasRoute("home/tree/{nodeId}", null)) {
                    slideInVertically { it } + fadeIn()
                } else {
                    slideInHorizontally { it } + fadeIn()
                }
            },
            exitTransition = {
                if (targetState.destination.hasRoute("home/tree/{nodeId}", null)) {
                    fadeOut()
                } else {
                    slideOutHorizontally { -it } + fadeOut()
                }
            },
            popEnterTransition = {
                if (initialState.destination.hasRoute("home/tree/{nodeId}", null)) {
                    fadeIn()
                } else {
                    slideInHorizontally { -it } + fadeIn()
                }
            },
            popExitTransition = {
                if (initialState.destination.hasRoute("home/tree/{nodeId}", null)) {
                    slideOutVertically { it } + fadeOut()
                } else {
                    slideOutHorizontally { it } + fadeOut()
                }
            },
        ) {
            composable("home") { Home(db, navController) }
            composable("home/tree/{nodeId}") { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getString("nodeId")
                Tree(db, navController, nodeId?.toInt())
            }
            composable("edit/{nodeId}") { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getString("nodeId")!!
                Edit(db, navController, nodeId.toInt())
            }
            composable("create/{nodeId}") { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getString("nodeId")!!
                Create(db, navController, nodeId.toInt())
            }
            composable("reorder/{nodeId}") { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getString("nodeId")!!
                Reorder(db, navController, nodeId.toInt())
            }
            composable("move/{nodeId}") { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getString("nodeId")!!
                Move(db, navController, nodeId.toInt())
            }
        }
    }
}
