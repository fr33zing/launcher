package dev.fr33zing.launcher.data.utility

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.room.withTransaction
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.createNodeWithPayload
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Note

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

suspend fun setupDefaultHomeScreen(db: AppDatabase) {
    val home = db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home)

    suspend fun homeNote(text: String) {
        db.createNodeWithPayload<Note>(home.nodeId, text)
    }

    suspend fun rootNote(text: String) {
        db.createNodeWithPayload<Note>(ROOT_NODE_ID, text, nodeMutateFunction = { it.order = -1 })
    }

    db.withTransaction {
        rootNote("Nodes in the Home directory show up on the home screen.")
        rootNote("Long press nodes to modify them or add new nodes adjacent to them.")
        rootNote("Tap directory nodes to expand or collapse them.")

        homeNote("Swipe up to enter tree view and see all your apps.")
        homeNote("Long press in empty space on the home screen to view the preferences page.")
    }
}
