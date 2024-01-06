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
    // Node text & spacing
    val fontSize = Preference(context, intPreferencesKey("fontSize"), 22, Int::sp)
    val spacing = Preference(context, intPreferencesKey("spacing"), 22, Int::dp)
    val indent = Preference(context, intPreferencesKey("indent"), 22, Int::dp)

    // Confirmation dialogs
    val askOnCreateNodeAccept =
        Preference(context, booleanPreferencesKey("askOnCreateNodeAccept"), false, ::noMap)
    val askOnCreateNodeReject =
        Preference(context, booleanPreferencesKey("askOnCreateNodeReject"), true, ::noMap)
    val askOnEditNodeAccept =
        Preference(context, booleanPreferencesKey("askOnEditNodeAccept"), false, ::noMap)
    val askOnEditNodeReject =
        Preference(context, booleanPreferencesKey("askOnEditNodeReject"), true, ::noMap)
    val askOnMoveNodeAccept =
        Preference(context, booleanPreferencesKey("askOnMoveNodeAccept"), false, ::noMap)
    val askOnMoveNodeReject =
        Preference(context, booleanPreferencesKey("askOnMoveNodeReject"), true, ::noMap)
    val askOnReorderNodesAccept =
        Preference(context, booleanPreferencesKey("askOnReorderNodesAccept"), false, ::noMap)
    val askOnReorderNodesReject =
        Preference(context, booleanPreferencesKey("askOnReorderNodesReject"), true, ::noMap)
}

private fun <T> noMap(value: T): T = value
