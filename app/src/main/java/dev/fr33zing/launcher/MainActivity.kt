package dev.fr33zing.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.createNewApplications
import dev.fr33zing.launcher.helper.getActivityInfos
import dev.fr33zing.launcher.helper.launcherApps
import dev.fr33zing.launcher.helper.userManager
import dev.fr33zing.launcher.ui.components.refreshNodeList
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Reorder
import dev.fr33zing.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    Main(db)

                    // Check for new apps
                    LaunchedEffect(Unit) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val activityInfos = getActivityInfos(applicationContext)
                            val newAppsAdded = db.createNewApplications(activityInfos)
                            if (newAppsAdded > 0) {
                                Log.d("", "Added $newAppsAdded new apps")
                                refreshNodeList()
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Main(db: AppDatabase) {
        val navController = rememberNavController()

        NavHost(
            navController,
            startDestination = "home",
            enterTransition = { slideInHorizontally() + fadeIn() },
            exitTransition = { slideOutHorizontally() + fadeOut() },
        ) {
            composable("home") { Home(db, navController) }
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
        }
    }
}
