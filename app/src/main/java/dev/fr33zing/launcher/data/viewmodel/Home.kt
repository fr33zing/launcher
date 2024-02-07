package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.viewmodel.state.TreeBrowserStateHolder
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltViewModel
class HomeViewModel @Inject constructor(private val db: AppDatabase) : ViewModel() {
    val isFirstRun = runBlocking { db.nodeDao().getNodeById(ROOT_NODE_ID) == null }

    val treeBrowser =
        TreeBrowserStateHolder(
            db,
            viewModelScope,
            initialRootNode = { db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home) },
        )

    fun activatePayload(context: Context, payload: Payload) {
        payload.activate(db, context)
    }
}
