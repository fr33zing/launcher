package dev.fr33zing.launcher.data.persistent.payloads

import androidx.annotation.Keep
import androidx.room.Entity

@Keep @Entity class Checkbox(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
