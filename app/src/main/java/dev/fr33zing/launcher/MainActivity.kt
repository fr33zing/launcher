package dev.fr33zing.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.createNewApplications
import dev.fr33zing.launcher.data.persistent.deleteNewApplicationsDirectoryIfEmpty
import dev.fr33zing.launcher.data.persistent.payloads.launcherApps
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import dev.fr33zing.launcher.data.persistent.payloads.userManager
import dev.fr33zing.launcher.data.utility.getActivityInfos
import dev.fr33zing.launcher.ui.components.Notices
import dev.fr33zing.launcher.ui.theme.LauncherTheme
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val TAG = "dev.fr33zing.launcher"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // TODO remove this once everything uses dependency injection
    @Inject lateinit var db: AppDatabase

    private lateinit var packagesInstalledAtLaunch: List<Pair<String, UserHandle>>

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

        // Keep track of what packages are installed at launch so we can save on database calls when
        // checking for new applications on resume or ACTION_PACKAGE_ADDED broadcast received.
        runBlocking {
            packagesInstalledAtLaunch =
                getActivityInfos(applicationContext).map {
                    Pair(it.componentName.packageName, it.user)
                }
        }

        setContent {
            BroadcastReceiver(Intent.ACTION_PACKAGE_ADDED) { checkForNewApplications() }

            LauncherTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box {
                        SetupNavigation(db)
                        Notices()
                    }
                }
            }
        }
    }

    private fun checkForNewApplications() {
        CoroutineScope(Dispatchers.IO).launch {
            val activityInfos =
                getActivityInfos(applicationContext).filter { activityInfo ->
                    packagesInstalledAtLaunch.none { alreadyInstalled ->
                        activityInfo.componentName.packageName == alreadyInstalled.first &&
                            activityInfo.user == alreadyInstalled.second
                    }
                }
            db.createNewApplications(activityInfos)
            db.deleteNewApplicationsDirectoryIfEmpty()
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
