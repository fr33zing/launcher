package com.example.mylauncher.data.persistent.payloads

import androidx.room.Entity

@Entity class Checkbox(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
