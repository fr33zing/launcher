package com.example.mylauncher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.mylauncher.data.AppDatabase
import com.example.mylauncher.helper.getAppList
import com.example.mylauncher.helper.launcherApps
import com.example.mylauncher.helper.userManager
import com.example.mylauncher.ui.util.Fade
import com.example.mylauncher.ui.pages.EditNode
import com.example.mylauncher.ui.pages.Home
import com.example.mylauncher.ui.theme.MyLauncherTheme
import kotlinx.coroutines.flow.flow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        userManager = getSystemService(Context.USER_SERVICE) as UserManager

        window.setDecorFitsSystemWindows(false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database")
            .build()

        setContent {
            // Check for new apps
            val appsFlow = flow { emit(getAppList(applicationContext)) }
            LaunchedEffect(Unit) {
                appsFlow.collect {
                    db.nodeDao()
                        .insertNewApps(it)
                }
            }

            MyLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Main(db)
                }
            }
        }
    }

}

@Composable
private fun Main(db: AppDatabase) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") { Fade { Home(db, navController) } }
        composable("edit/node/{uuid}") { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("uuid")
                ?: throw Exception("No value provided for navigation argument: filename")
            Fade { EditNode(db, navController, uuid) }
        }
    }
}