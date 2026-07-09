// Top-level build file. Plugin versions are managed in gradle/libs.versions.toml.
plugins {
    // Kotlin support is built into AGP 9.0+, so kotlin.android is not declared.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
