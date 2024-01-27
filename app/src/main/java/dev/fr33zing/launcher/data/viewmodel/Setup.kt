package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.autoCategorizeNewApplications
import dev.fr33zing.launcher.data.persistent.createNewApplications
import dev.fr33zing.launcher.data.persistent.createNodeWithPayload
import dev.fr33zing.launcher.data.persistent.deleteNewApplicationsDirectoryIfEmpty
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Note
import dev.fr33zing.launcher.data.utility.getActivityInfos
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class SetupState(val remainingAppsToCategorize: Int, val progress: Float)

@HiltViewModel
class SetupViewModel @Inject constructor(private val db: AppDatabase) : ViewModel() {
    private var applicationsCategorized = 0
    private val _flow = MutableSharedFlow<SetupState>()
    val flow = _flow.asSharedFlow()

    suspend fun autoCategorizeApplications(context: Context) {
        val activityInfos = getActivityInfos(context)
        val applicationCount = db.createNewApplications(activityInfos)

        db.autoCategorizeNewApplications(context) {
            viewModelScope.launch {
                applicationsCategorized++

                val remainingAppsToCategorize = applicationCount - applicationsCategorized
                val progress = applicationsCategorized.toFloat() / applicationCount
                val state = SetupState(remainingAppsToCategorize, progress)

                if (applicationsCategorized == 0) db.deleteNewApplicationsDirectoryIfEmpty()
                _flow.emit(state)
            }
        }
    }

    suspend fun addNewUserInstructionNodes() =
        db.withTransaction {
            val home = db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home)

            suspend fun homeNote(text: String) {
                db.createNodeWithPayload<Note>(home.nodeId, text)
            }

            suspend fun rootNote(text: String) {
                db.createNodeWithPayload<Note>(
                    ROOT_NODE_ID,
                    text,
                    nodeMutateFunction = { it.order = -1 }
                )
            }

            homeNote("Swipe up to enter tree view.")
            homeNote("Long press in empty space to edit preferences.")

            rootNote("Pull down to search.")
            rootNote("Pinch to zoom out.")
            rootNote("Tap directories to expand or collapse them.")
            rootNote("Long press items to modify them or add new items adjacent to them.")
            rootNote("Items in the Home directory show up on the home screen.")
        }
}
