package com.example.mylauncher.helper

import androidx.compose.ui.Modifier

// https://stackoverflow.com/a/72554087
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
