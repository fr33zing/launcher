package com.example.mylauncher.data.persistent.payloads

import androidx.room.Entity

@Entity class Reference(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
