package dev.fr33zing.launcher.data.persistent.payloads

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.fr33zing.launcher.ui.utility.UserEditable

@Keep
@Entity
class File(
    payloadId: Int,
    nodeId: Int,
    @ColumnInfo(defaultValue = "")
    @UserEditable("File path", locked = true)
    var filePath: String = "",
    @ColumnInfo(defaultValue = "")
    @UserEditable("Open with package", locked = true, userCanUnlock = true)
    var openWithPackageName: String = "",
) : Payload(payloadId, nodeId)
