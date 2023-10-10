package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.fr33zing.launcher.data.persistent.AppDatabase

@Entity
abstract class Payload(
    @PrimaryKey(autoGenerate = true) val payloadId: Int,
    val nodeId: Int,
) {
    open fun activate(db: AppDatabase, context: Context) {}

    open fun preInsert() {}

    open fun preUpdate() {}

    open fun preDelete() {}
}
