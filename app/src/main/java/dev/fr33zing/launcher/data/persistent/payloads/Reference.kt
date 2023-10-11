package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity
class Reference(payloadId: Int, nodeId: Int, var targetId: Int? = null) :
    Payload(payloadId, nodeId) {

    override fun activate(db: AppDatabase, context: Context) {
        if (targetId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            db.nodeDao().getNodeById(targetId!!)?.let { target ->
                db.getPayloadByNodeId(target.kind, target.nodeId)?.activate(db, context)
            }
        }
    }
}
