package dev.fr33zing.launcher.data.persistent.payloads

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Date

@Keep
@Entity
class Checkbox(
    payloadId: Int,
    nodeId: Int,
    @ColumnInfo(defaultValue = "false") var checked: Boolean = false,
    var uncheckedOn: Date? = null,
    var checkedOn: Date? = null,
) : Payload(payloadId, nodeId)
