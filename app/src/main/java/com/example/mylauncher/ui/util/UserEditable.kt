package com.example.mylauncher.ui.util

import kotlin.reflect.KMutableProperty0

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class UserEditable(val label: String, val supportingText: String = "")

fun <V> KMutableProperty0<V>.getUserEditableAnnotation(): UserEditable =
    (annotations.firstOrNull { it is UserEditable } as UserEditable?)
        ?: throw Exception("Missing @UserEditable annotation")
