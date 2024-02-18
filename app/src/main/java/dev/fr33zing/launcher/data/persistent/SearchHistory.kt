package dev.fr33zing.launcher.data.persistent

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val HISTORY_FILE_NAME = "search_history"
private const val HISTORY_LENGTH = 25

class SearchHistory(context: Context) {
    private val file = File(context.cacheDir, HISTORY_FILE_NAME)
    private val newLine = System.getProperty("line.separator") ?: "\n"

    private val _flow = MutableStateFlow(get())
    val flow = _flow.asStateFlow()

    fun get(): List<String> {
        ensureFileExists()
        return file.readLines()
    }

    fun add(query: String, scope: CoroutineScope) {
        ensureFileExists()
        val lines = ArrayDeque(file.readLines())
        if (query in lines) lines.remove(query)
        lines.addFirst(query.replace(newLine, ""))
        while (lines.size > HISTORY_LENGTH) lines.removeLast()
        scope.launch { _flow.emit(lines) }
        file.writeText(lines.joinToString(newLine))
    }

    private fun ensureFileExists() {
        if (!file.exists()) file.createNewFile()
    }
}
