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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.autoCategorizeNewApplications
import dev.fr33zing.launcher.data.persistent.createNewApplications
import dev.fr33zing.launcher.data.persistent.payloads.launcherApps
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import dev.fr33zing.launcher.data.persistent.payloads.userManager
import dev.fr33zing.launcher.data.utility.getActivityInfos
import dev.fr33zing.launcher.ui.components.Notices
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Move
import dev.fr33zing.launcher.ui.pages.Reorder
import dev.fr33zing.launcher.ui.pages.Tree
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.LauncherTheme
import dev.fr33zing.launcher.ui.utility.mix
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val TAG = "dev.fr33zing.launcher"

val GoHomeSubject = PublishSubject.create<Unit>()

class MainActivity : ComponentActivity() {

    override fun onPause() {
        super.onPause()
        GoHomeSubject.onNext(Unit)
    }

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
                    var remainingAppsToCategorize by remember { mutableStateOf<Int?>(null) }
                    var initialAppsToCategorize by remember { mutableStateOf<Float?>(null) }

                    if (remainingAppsToCategorize == 0 || remainingAppsToCategorize == null) {
                        Box {
                            Main(db)
                            Notices()
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                buildString {
                                    append("Automatically categorizing applications.")
                                    append("\n\n")
                                    append(
                                        "This is done slowly to reduce the load on any network services used to determine application categories."
                                    )
                                    append("\n\n")
                                    append("Remaining: $remainingAppsToCategorize")
                                },
                                textAlign = TextAlign.Center
                            )
                            if (
                                remainingAppsToCategorize != null && initialAppsToCategorize != null
                            ) {
                                Spacer(Modifier.height(32.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(0.5f),
                                    progress =
                                        1 - remainingAppsToCategorize!! / initialAppsToCategorize!!,
                                    color = Foreground,
                                    trackColor = Background.mix(Foreground, 0.1f),
                                )
                            }
                        }
                    }

                    // Check for new apps
                    LaunchedEffect(Unit) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val isFirstRun = db.nodeDao().getNodeById(ROOT_NODE_ID) == null
                            val activityInfos = getActivityInfos(applicationContext)
                            val newApps = db.createNewApplications(activityInfos)

                            if (isFirstRun) {
                                initialAppsToCategorize = newApps.toFloat()
                                remainingAppsToCategorize = newApps
                                db.autoCategorizeNewApplications(applicationContext) {
                                    remainingAppsToCategorize = remainingAppsToCategorize!! - 1
                                }
                            } else remainingAppsToCategorize = 0
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
