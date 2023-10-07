package dev.fr33zing.launcher.data.persistent.payloads

import androidx.room.Entity

@Entity class Directory(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
