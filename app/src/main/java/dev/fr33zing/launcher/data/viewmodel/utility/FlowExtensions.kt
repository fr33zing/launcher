package dev.fr33zing.launcher.data.viewmodel.utility

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformLatest

private const val STAGGER_MS: Long = 25 // TODO: Add a user preference for this

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<List<T>>.stagger() = transformLatest {
    for (i in it.indices) {
        delay(STAGGER_MS)
        emit(it.slice(0..i))
    }
}

fun <E> Flow<E>.maybeFilter(predicate: ((E) -> Boolean)?) = predicate?.let { filter(it) } ?: this
