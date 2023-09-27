package com.example.mylauncher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.mylauncher.data.AppDatabase
import com.example.mylauncher.helper.conditional
import com.example.mylauncher.helper.getActivityInfos
import com.example.mylauncher.helper.launcherApps
import com.example.mylauncher.helper.userManager
import com.example.mylauncher.ui.pages.Edit
import com.example.mylauncher.ui.pages.Home
import com.example.mylauncher.ui.theme.MyLauncherTheme
import kotlinx.coroutines.flow.flow

lateinit var dialogVisible: MutableState<Boolean>
const val blurDialogBackdrop = false

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
            // Setup dialog backdrop blur
            // TODO determine why this animation is slow only the first time it runs
            dialogVisible = remember { mutableStateOf(false) }
            val dialogBackdropBlurRadius by animateDpAsState(
                targetValue = if (dialogVisible.value) 2.dp else 0.dp,
                animationSpec = snap(80),
                label = "dialog backdrop blur radius"
            )

            // Check for new apps
            val appsFlow = flow { emit(getActivityInfos(applicationContext)) }
            LaunchedEffect(Unit) {
                appsFlow.collect {
                    db.nodeDao()
                        .insertNewApps(it)
                }
            }

            MyLauncherTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxSize()
                        .conditional(blurDialogBackdrop) { blur(dialogBackdropBlurRadius) },
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