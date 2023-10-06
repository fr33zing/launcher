package com.example.mylauncher.data.persistent

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
abstract class Payload(
    @PrimaryKey(autoGenerate = true) val payloadId: Int,
    val nodeId: Int,
)
