package dev.fr33zing.launcher.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.getOrCreateSingletonDirectory
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.viewmodel.utility.TreeBrowserStateHolder
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val db: AppDatabase) : ViewModel() {
    //    val nodePayloads =
    //        flow {
    //                val homeNode = db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home)
    //                val childNodes = db.nodeDao().getChildNodes(homeNode.nodeId)
    //                val listStateHolder = NodePayloadListStateHolder(db, childNodes)
    //                emitAll(listStateHolder.flow)
    //            }
    //            .stateIn(
    //                scope = viewModelScope,
    //                started = SharingStarted.Eagerly,
    //                initialValue = emptyArray(),
    //            )

    val treeBrowser =
        TreeBrowserStateHolder(
            db,
            viewModelScope,
            initialRootNode = { db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home) },
        )
}
