package dev.fr33zing.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
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
import dev.fr33zing.launcher.data.utility.addNewUserInstructionNodes
import dev.fr33zing.launcher.data.utility.getActivityInfos
import dev.fr33zing.launcher.ui.components.Notices
import dev.fr33zing.launcher.ui.pages.Create
import dev.fr33zing.launcher.ui.pages.Edit
import dev.fr33zing.launcher.ui.pages.Home
import dev.fr33zing.launcher.ui.pages.Move
import dev.fr33zing.launcher.ui.pages.Preferences
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
private var goHomeOnNextPause = true

fun doNotGoHomeOnNextPause() {
    goHomeOnNextPause = false
}

class MainActivity : ComponentActivity() {
    lateinit var db: AppDatabase

    override fun onPause() {
        super.onPause()
        if (goHomeOnNextPause) GoHomeSubject.onNext(Unit) else goHomeOnNextPause = true
    }

    override fun onResume() {
        super.onResume()
        checkForNewApplications()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainPackageManager = packageManager
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        userManager = getSystemService(Context.USER_SERVICE) as UserManager

        window.setDecorFitsSystemWindows(false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()

        setContent {
            BroadcastReceiver(Intent.ACTION_PACKAGE_ADDED) { checkForNewApplications() }

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
                                addNewUserInstructionNodes(db)
                            } else remainingAppsToCategorize = 0
                        }
                    }
                }
            }
        }
    }

    private fun checkForNewApplications() {
        CoroutineScope(Dispatchers.IO).launch {
            val activityInfos = getActivityInfos(applicationContext)
            db.createNewApplications(activityInfos)
        }
    }

    @Composable
    private fun Main(db: AppDatabase) {
        // TODO fix bug that causes tree to slide horizontally when pressing home button
        val navController = rememberNavController()

        DisposableEffect(Unit) {
            val subscription =
                GoHomeSubject.subscribe {
                    if (navController.currentDestination?.route != "home")
                        navController.navigate("home")
                }
            onDispose { subscription.dispose() }
        }

        fun NavBackStackEntry.hasTreeRoute() = destination.route?.startsWith("home/tree/") == true
        fun NavBackStackEntry.nodeIdOrNull() = arguments?.getString("nodeId")?.toInt()
        fun NavBackStackEntry.nodeId() = nodeIdOrNull() ?: throw Exception("nodeId is null")

        NavHost(
            navController,
            startDestination = "home",
            enterTransition = {
                if (targetState.hasTreeRoute()) slideInVertically { it } + fadeIn()
                else slideInHorizontally { it } + fadeIn()
            },
            exitTransition = {
                if (targetState.hasTreeRoute()) fadeOut()
                else slideOutHorizontally { -it } + fadeOut()
            },
            popEnterTransition = {
                if (initialState.hasTreeRoute()) fadeIn()
                else slideInHorizontally { -it } + fadeIn()
            },
            popExitTransition = {
                if (initialState.hasTreeRoute()) slideOutVertically { it } + fadeOut()
                else slideOutHorizontally { it } + fadeOut()
            },
        ) {
            composable("settings") { Preferences(db) }
            composable("home") { Home(db, navController) }
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
            composable("move/{nodeId}") { backStackEntry ->
                Move(db, navController, backStackEntry.nodeId())
            }
        }
    }

    @Composable
    fun BroadcastReceiver(filter: String, onReceive: (Intent?) -> Unit) {
        val context = LocalContext.current
        val onReceiveState by rememberUpdatedState(onReceive)

        DisposableEffect(context, filter) {
            val intentFilter = IntentFilter(filter)

            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        onReceiveState(intent)
                    }
                }

            context.registerReceiver(receiver, intentFilter)

            onDispose { context.unregisterReceiver(receiver) }
        }
    }
}
