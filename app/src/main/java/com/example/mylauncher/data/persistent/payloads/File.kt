package com.example.mylauncher.data.persistent.payloads

import androidx.room.Entity

@Entity class File(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
