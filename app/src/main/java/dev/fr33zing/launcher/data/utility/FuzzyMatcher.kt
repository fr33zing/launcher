package dev.fr33zing.launcher.data.utility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.min

@Composable
fun <T> rememberFuzzyMatcher(elements: List<T>, toStringFn: (T) -> String) = remember {
    FuzzyMatcher(elements, toStringFn)
}

class FuzzyMatcher<T>(elements: List<T>, toStringFn: (T) -> String) {
    data class Substring(val text: String, val matches: Boolean, val index: Int)

    data class Result<T>(val score: Int, val substrings: List<Substring>, val element: T)

    private val elementStrings = elements.associateWith { toStringFn(it) }

    fun match(query: String): List<Result<T>> =
        if (query.isEmpty()) listOf()
        else
            elementStrings
                .map { (element, elementString) ->
                    data class Match(var start: Int = 0, var length: Int = 0)

                    val longestMatch = Match()
                    var currentMatch = Match()
                    var matching = false

                    for (i in 0 ..< min(query.length, elementString.length)) {
                        val c = elementString[i]
                        val q = query[currentMatch.length]

                        if (c.equals(q, ignoreCase = true)) {
                            if (!matching) currentMatch.start = i
                            matching = true
                            currentMatch.length++

                            if (currentMatch.length > longestMatch.length) {
                                longestMatch.start = currentMatch.start
                                longestMatch.length = currentMatch.length
                            }
                        } else {
                            if (matching) currentMatch = Match()
                            matching = false
                        }
                    }

                    val substrings =
                        if (longestMatch.length == 0)
                            listOf(
                                Substring(
                                    elementString,
                                    index = longestMatch.start,
                                    matches = false,
                                )
                            )
                        else if (longestMatch.start == 0) {
                            listOf(
                                Substring(
                                    elementString.substring(0, longestMatch.length),
                                    index = 0,
                                    matches = true,
                                ),
                                Substring(
                                    elementString.substring(longestMatch.length),
                                    index = longestMatch.start,
                                    matches = false,
                                )
                            )
                        } else {
                            listOf(
                                Substring(
                                    elementString.substring(0, longestMatch.start),
                                    index = 0,
                                    matches = false,
                                ),
                                Substring(
                                    elementString.substring(
                                        longestMatch.start,
                                        longestMatch.start + longestMatch.length
                                    ),
                                    index = longestMatch.start,
                                    matches = true,
                                ),
                                Substring(
                                    elementString.substring(
                                        longestMatch.start + longestMatch.length
                                    ),
                                    index = longestMatch.start + longestMatch.length,
                                    matches = false,
                                )
                            )
                        }

                    Result(longestMatch.length, substrings, element)
                }
                .filter { it.score > 0 }
                .sortedWith(
                    compareByDescending<Result<T>> { it.score }
                        .thenBy { it.substrings.first { substring -> substring.matches }.index }
                )
}
