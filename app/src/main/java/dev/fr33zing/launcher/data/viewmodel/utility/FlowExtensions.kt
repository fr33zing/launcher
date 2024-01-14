package dev.fr33zing.launcher.data.viewmodel.utility

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

fun <E> Flow<E>.maybeFilter(predicate: ((E) -> Boolean)?) = predicate?.let { filter(it) } ?: this
