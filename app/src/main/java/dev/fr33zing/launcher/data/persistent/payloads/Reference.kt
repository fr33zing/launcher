package dev.fr33zing.launcher.data.persistent.payloads

import androidx.room.Entity

@Entity
class Reference(payloadId: Int, nodeId: Int, var targetId: Int? = null) :
    Payload(payloadId, nodeId)
