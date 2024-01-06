package dev.fr33zing.launcher.data.utility

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

// TODO add setting to choose default clock and calendar app

fun launchClockApplication(context: Context) {
    val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
    clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(clockIntent)
}

fun launchCalendarApplication(context: Context) {
    val calendarIntent = Intent(Intent.ACTION_MAIN)
    calendarIntent.addCategory(Intent.CATEGORY_APP_CALENDAR)
    context.startActivity(calendarIntent)
}
