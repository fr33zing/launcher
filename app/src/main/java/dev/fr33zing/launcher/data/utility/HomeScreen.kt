package dev.fr33zing.launcher.data.utility

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.sendNotice

// TODO add setting to choose default clock and calendar app

fun launchClockApplication(
    context: Context,
    applicationPreference: String,
) {
    if (applicationPreference.isNotEmpty()) {
        mainPackageManager.getLaunchIntentForPackage(applicationPreference)?.let {
            context.startActivity(it)
        }
            ?: sendNotice(
                "invalid-clock-application",
                "Invalid clock application: $applicationPreference",
                NoticeKind.Error,
            )
    } else {
        val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(clockIntent)
    }
}

fun launchCalendarApplication(
    context: Context,
    applicationPreference: String,
) {
    if (applicationPreference.isNotEmpty()) {
        mainPackageManager.getLaunchIntentForPackage(applicationPreference)?.let {
            context.startActivity(it)
        }
            ?: sendNotice(
                "invalid-calendar-application",
                "Invalid calendar application: $applicationPreference",
                NoticeKind.Error,
            )
    } else {
        val calendarIntent = Intent(Intent.ACTION_MAIN)
        calendarIntent.addCategory(Intent.CATEGORY_APP_CALENDAR)
        context.startActivity(calendarIntent)
    }
}
