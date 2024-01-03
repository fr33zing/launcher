package dev.fr33zing.launcher.data.persistent

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

val Context.preferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "preferences")

class Preference<UnderlyingType, MappedType>(
    private val context: Context,
    private val key: Preferences.Key<UnderlyingType>,
    private val default: UnderlyingType,
    map: (UnderlyingType) -> MappedType,
) {

    private val flow: Flow<UnderlyingType> =
        context.preferencesDataStore.data.map { it[key] ?: default }
    private val mappedFlow: Flow<MappedType> = flow.map(map)
    val mappedDefault = map(default)

    val state
        @Composable get() = mappedFlow.collectAsState(initial = mappedDefault)

    suspend fun get(): UnderlyingType {
        return flow.last()
    }

    suspend fun set(value: UnderlyingType) {
        context.preferencesDataStore.edit { it[key] = value }
    }
}

class Preferences(private val context: Context) {
    val fontSize = Preference(context, intPreferencesKey("fontSize"), 22, Int::sp)
    val spacing = Preference(context, intPreferencesKey("spacing"), 22, Int::dp)
    val indent = Preference(context, intPreferencesKey("indent"), 22, Int::dp)

    @Serializable
    data class Export(
        val fontSize: Int,
        val spacing: Int,
        val indent: Int,
    ) {
        fun toYaml(): String = Yaml.default.encodeToString(this)
    }

    suspend fun export(): Export = Export(fontSize.get(), spacing.get(), indent.get())
}
