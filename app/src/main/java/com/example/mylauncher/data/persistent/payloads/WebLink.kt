package com.example.mylauncher.data.persistent.payloads

import androidx.room.Entity

@Entity class WebLink(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
