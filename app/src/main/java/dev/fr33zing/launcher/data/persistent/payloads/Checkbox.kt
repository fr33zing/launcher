package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Keep
@Entity
class Checkbox(
    payloadId: Int,
    nodeId: Int,
    @ColumnInfo(defaultValue = "false") var checked: Boolean = false,
    var uncheckedOn: Date? = null,
    var checkedOn: Date? = null,
) : Payload(payloadId, nodeId) {
    override fun activate(db: AppDatabase, context: Context) {
        checked = !checked
        if (checked) checkedOn = Date() else uncheckedOn = Date()
        this.let { payload -> CoroutineScope(Dispatchers.IO).launch { db.update(payload) } }
    }
}
