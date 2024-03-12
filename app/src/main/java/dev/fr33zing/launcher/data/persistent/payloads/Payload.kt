package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.fr33zing.launcher.data.persistent.AppDatabase
import java.util.Date

@Keep
@Entity
abstract class Payload(
    @PrimaryKey(autoGenerate = true) val payloadId: Int,
    val nodeId: Int,
    @ColumnInfo(defaultValue = "0") var created: Date = Date(),
    @ColumnInfo(defaultValue = "0") var updated: Date = Date(),
) {
    open fun activate(
        db: AppDatabase,
        context: Context,
    ) {}

    open fun preInsert() {
        created = Date()
    }

    open fun preUpdate() {
        updated = Date()
    }

    open fun preDelete() {}
}
