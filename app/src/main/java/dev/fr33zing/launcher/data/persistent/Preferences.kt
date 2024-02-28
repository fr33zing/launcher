package dev.fr33zing.launcher.data.persistent

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val PREFERENCES_DATASTORE_NAME = "user_preferences"

val Context.preferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(PREFERENCES_DATASTORE_NAME)

class Preference<UnderlyingType, MappedType>(
    private val context: Context,
    private val key: Preferences.Key<UnderlyingType>,
    val default: UnderlyingType,
    map: (UnderlyingType) -> MappedType,
) {
    val flow: Flow<UnderlyingType> =
        context.preferencesDataStore.data.map {
            if (it[key] == null)
                context.preferencesDataStore.edit { preferences -> preferences[key] = default }
            it[key] ?: default
        }
    private val mappedFlow: Flow<MappedType> = flow.map(map)
    val mappedDefault = map(default)

    val state
        @Composable get() = mappedFlow.collectAsState(initial = mappedDefault)

    suspend fun set(value: UnderlyingType) {
        context.preferencesDataStore.edit { it[key] = value }
    }
}

class Preferences(context: Context) {
    val nodeAppearance = NodeAppearancePreferences(context)
    val confirmationDialogs = ConfirmationDialogPreferences(context)
    val home = HomePreferences(context)
    val search = SearchPreferences(context)
    val notices = NoticePreferences(context)
    val debug = DebugPreferences(context)
}

class NodeAppearancePreferences(context: Context) {
    val fontSize = Preference(context, intPreferencesKey("nodeAppearance.fontSize"), 22, Int::sp)
    val spacing = Preference(context, intPreferencesKey("nodeAppearance.spacing"), 22, Int::dp)
    val indent = Preference(context, intPreferencesKey("nodeAppearance.indent"), 22, Int::dp)
}

class HomePreferences(context: Context) {
    class DefaultApplicationPreferences(context: Context) {
        private fun defaultApplicationPreference(context: Context, key: String) =
            Preference(context, stringPreferencesKey("home.defaultApps.$key"), "", ::noMap)

        val clock = defaultApplicationPreference(context, "clock")
        val calendar = defaultApplicationPreference(context, "calendar")
    }

    val use24HourTime =
        Preference(context, booleanPreferencesKey("home.use24HourTime"), false, ::noMap)
    val defaultApplications = DefaultApplicationPreferences(context)
}

class ConfirmationDialogPreferences(context: Context) {
    class PreferenceGroup(context: Context, key: String) {
        private val askOnAcceptKey = "confirmationDialogs.$key.askOnAccept"
        private val askOnRejectKey = "confirmationDialogs.$key.askOnReject"
        val askOnAccept = Preference(context, booleanPreferencesKey(askOnAcceptKey), false, ::noMap)
        val askOnReject = Preference(context, booleanPreferencesKey(askOnRejectKey), true, ::noMap)
    }

    val createNode = PreferenceGroup(context, "createNode")
    val editNode = PreferenceGroup(context, "editNode")
    val moveNode = PreferenceGroup(context, "moveNode")
    val reorderNodes = PreferenceGroup(context, "reorderNodes")
}

class SearchPreferences(context: Context) {
    val webSearchApplication =
        Preference(context, stringPreferencesKey("search.webSearchApplication"), "", ::noMap)
}

class NoticePreferences(context: Context) {
    val positionAtTop =
        Preference(context, booleanPreferencesKey("notices.positionAtTop"), false, ::noMap)
    val durationSeconds =
        Preference(context, intPreferencesKey("notices.durationSeconds"), 3, ::noMap)
}

class DebugPreferences(context: Context) {
    val useNewTree = Preference(context, booleanPreferencesKey("useNewTree"), true, ::noMap)
}

private fun <T> noMap(value: T): T = value
