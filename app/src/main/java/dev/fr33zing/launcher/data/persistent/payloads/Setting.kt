package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.utility.UserEditable

@Keep
@Entity
class Setting(
    payloadId: Int,
    nodeId: Int,
    @UserEditable(label = "Setting", locked = true) var setting: String = "",
) : Payload(payloadId, nodeId) {
    override fun activate(
        db: AppDatabase,
        context: Context,
    ) {
        context.startActivity(Intent(setting))
    }
}
