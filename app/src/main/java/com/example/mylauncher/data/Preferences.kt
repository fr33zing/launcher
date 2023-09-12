package com.example.mylauncher.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "preferences"
)

class Preferences(private val context: Context) {
    companion object {
        val fontSizeDefault = 22.sp
        val spacingDefault = 22.dp
        val indentDefault = 30.dp
    }

    // Font size
    private val fontSizeKey = intPreferencesKey("fontSize")
    private val fontSizeFlow: Flow<TextUnit> =
        context.preferencesDataStore.data.map { preferences ->
            preferences[fontSizeKey]?.sp ?: Companion.fontSizeDefault
        }

    @Composable
    fun getFontSize(): State<TextUnit> = fontSizeFlow.collectAsState(Companion.fontSizeDefault)
    suspend fun setFontSize(value: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[fontSizeKey] = value
        }
    }

    // Spacing
    private val spacingKey = intPreferencesKey("fontSize")
    private val spacingFlow: Flow<Dp> =
        context.preferencesDataStore.data.map { preferences ->
            preferences[spacingKey]?.dp ?: Companion.spacingDefault
        }

    @Composable
    fun getSpacing(): State<TextUnit> = fontSizeFlow.collectAsState(Companion.fontSizeDefault)
    suspend fun setSpacing(value: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[fontSizeKey] = value
        }
    }

    // Indent
    private val indentKey = intPreferencesKey("fontSize")
    private val indentFlow: Flow<Dp> =
        context.preferencesDataStore.data.map { preferences ->
            preferences[indentKey]?.dp ?: Companion.indentDefault
        }

    @Composable
    fun getIndent(): State<TextUnit> = fontSizeFlow.collectAsState(Companion.fontSizeDefault)
    suspend fun setIndent(value: Int) {
        context.preferencesDataStore.edit { preferences ->
            preferences[fontSizeKey] = value
        }
    }

}