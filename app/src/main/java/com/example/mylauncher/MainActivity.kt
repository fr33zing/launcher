package com.example.mylauncher

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
import com.example.mylauncher.data.persistent.AppDatabase
import com.example.mylauncher.data.persistent.createNewApplications
import com.example.mylauncher.helper.getActivityInfos
import com.example.mylauncher.helper.launcherApps
import com.example.mylauncher.helper.userManager
import com.example.mylauncher.ui.pages.Edit
import com.example.mylauncher.ui.pages.Home
import com.example.mylauncher.ui.theme.MyLauncherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

val NewAppsAdded = Channel<Int>()

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
            MyLauncherTheme {
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
                            if (newAppsAdded > 0) NewAppsAdded.send(newAppsAdded)
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
        }
    }
}
