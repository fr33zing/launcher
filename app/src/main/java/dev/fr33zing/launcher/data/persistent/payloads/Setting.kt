package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.util.UserEditable

@Entity
class Setting(
    payloadId: Int,
    nodeId: Int,
    @UserEditable(label = "Setting", locked = true) var setting: String = ""
) : Payload(payloadId, nodeId) {

    override fun activate(db: AppDatabase, context: Context) {

        // context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }
}
