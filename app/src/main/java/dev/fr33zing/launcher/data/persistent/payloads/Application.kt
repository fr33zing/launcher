package dev.fr33zing.launcher.data.persistent.payloads

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserManager
import androidx.room.Entity
import androidx.room.Ignore
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.util.UserEditable

lateinit var mainPackageManager: PackageManager
lateinit var launcherApps: LauncherApps
lateinit var userManager: UserManager

@Entity
class Application(
    payloadId: Int,
    nodeId: Int,
    @UserEditable("Application name", locked = true) var appName: String = "",
    @UserEditable("Package name", locked = true) var packageName: String = "",
    @UserEditable("Activity class name", locked = true, userCanUnlock = true)
    var activityClassName: String = "",
    @UserEditable("User handle", locked = true, userCanUnlock = true) var userHandle: String = "",
) : Payload(payloadId, nodeId) {
    constructor(
        payloadId: Int,
        nodeId: Int,
        activityInfo: LauncherActivityInfo
    ) : this(
        payloadId = payloadId,
        nodeId = nodeId,
        appName = activityInfo.label.toString(),
        packageName = activityInfo.applicationInfo.packageName,
        activityClassName = activityInfo.componentName.className,
        userHandle = activityInfo.user.toString()
    )

    enum class Status(val reason: String) {
        Valid("<valid>"),
        MissingPackage("the package is missing"),
        MissingActivity("the activity class name is invalid"),
        MissingProfile("the user profile is missing")
    }

    @Ignore private var _status: Status? = null

    val status
        get(): Status {
            if (_status == null) _status = computeStatus()
            return _status!!
        }

    private fun computeStatus(): Status {
        val packageInfo =
            try {
                mainPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            } catch (e: PackageManager.NameNotFoundException) {
                return Status.MissingPackage
            }

        if (packageInfo.activities.none { it.name == activityClassName })
            return Status.MissingActivity

        if (userManager.userProfiles.none { it.toString() == userHandle })
            return Status.MissingProfile

        return Status.Valid
    }

    override fun activate(db: AppDatabase, context: Context) =
        if (status == Status.Valid) {
            val userHandle =
                userManager.userProfiles.first { it.toString() == userHandle }
                    ?: throw Exception("Missing user profile")
            val activityList = launcherApps.getActivityList(packageName, userHandle)
            val componentName = ComponentName(packageName, activityList[activityList.size - 1].name)
            launcherApps.startMainActivity(componentName, userHandle, null, null)
        } else {
            sendNotice(
                "app-invalid:${nodeId}",
                "Cannot launch application because ${status.reason}."
            )
        }
}
