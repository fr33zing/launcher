package dev.fr33zing.launcher.data.persistent.payloads

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.fr33zing.launcher.ui.utility.UserEditable

@Keep
@Entity
class Note(
    payloadId: Int,
    nodeId: Int,
    @ColumnInfo(defaultValue = "") @UserEditable(label = "Note body") var body: String = "",
) : Payload(payloadId, nodeId)
