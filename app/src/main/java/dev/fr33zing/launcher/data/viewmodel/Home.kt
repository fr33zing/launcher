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
    val treeBrowser =
        TreeBrowserStateHolder(
            db,
            viewModelScope,
            initialRootNode = { db.getOrCreateSingletonDirectory(Directory.SpecialMode.Home) },
        )
}
