package dev.fr33zing.launcher.data.viewmodel.state

import androidx.compose.runtime.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformLatest

private const val STAGGER_MS: Long = 25

/** Makes a list of items appear to load one at a time. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<List<T>>.stagger(shouldStagger: State<Boolean>) = transformLatest {
    if (!shouldStagger.value) {
        emit(it)
    } else {
        delay(STAGGER_MS)

        for (i in it.indices) {
            if (!shouldStagger.value) {
                emit(it)
                break
            }

            emit(it.slice(0..i))
            if (i < it.indices.last) delay(STAGGER_MS)
        }
    }
}

fun <E> Flow<E>.maybeFilter(predicate: ((E) -> Boolean)?) = predicate?.let { filter(it) } ?: this
