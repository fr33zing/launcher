package com.example.mylauncher.data.persistent.payloads

import androidx.room.Entity

@Entity class Note(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
