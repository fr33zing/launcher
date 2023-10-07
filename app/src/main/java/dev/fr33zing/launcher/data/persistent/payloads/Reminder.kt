package dev.fr33zing.launcher.data.persistent.payloads

import androidx.room.Entity

@Entity class Reminder(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
