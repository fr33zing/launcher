package dev.fr33zing.launcher.data.utility

import androidx.room.withTransaction
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.createNodeWithPayload
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Note

suspend fun addNewUserInstructionNodes(db: AppDatabase) {
    val home = db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home)

    suspend fun homeNote(text: String) {
        db.createNodeWithPayload<Note>(home.nodeId, text)
    }

    suspend fun rootNote(text: String) {
        db.createNodeWithPayload<Note>(ROOT_NODE_ID, text, nodeMutateFunction = { it.order = -1 })
    }

    db.withTransaction {
        homeNote("Swipe up to enter tree view.")
        homeNote("Long press in empty space to edit preferences.")

        rootNote("Tap directory nodes to expand or collapse them.")
        rootNote("Long press nodes to modify them or add new nodes adjacent to them.")
        rootNote("Nodes in the Home directory show up on the home screen.")
    }
}
