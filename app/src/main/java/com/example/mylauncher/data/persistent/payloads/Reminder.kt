package com.example.mylauncher.data.persistent.payloads

import androidx.room.Entity

@Entity class Reminder(payloadId: Int, nodeId: Int) : Payload(payloadId, nodeId)
