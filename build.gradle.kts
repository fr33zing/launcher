plugins {
    alias(libs.plugins.com.diffplug.spotless)
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
    alias(libs.plugins.com.google.dagger.hilt.android) apply false
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        ktlint()
            .customRuleSets(
                listOf("com.twitter.compose.rules:ktlint:0.0.26"),
            ).editorConfigOverride(
                mapOf(
                    "max_line_length" to "120",
                    "ktlint_code_style" to "ktlint_official",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                ),
            )
    }
    kotlinGradle { ktlint() }
}
