package dev.fr33zing.launcher.ui.utility

import androidx.annotation.Keep
import kotlin.reflect.KMutableProperty0

@Keep
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class UserEditable(
    val label: String,
    val supportingText: String = "",
    val locked: Boolean = false,
    val userCanUnlock: Boolean = false
)

fun <V> KMutableProperty0<V>.getUserEditableAnnotation(): UserEditable =
    (annotations.firstOrNull { it is UserEditable } as UserEditable?)
        ?: throw Exception("Missing @UserEditable annotation")
