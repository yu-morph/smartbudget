// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("verify") {
    group = "verification"
    description = "Formats Kotlin sources, then runs tests and static analysis."

    dependsOn(
        ":app:ktlintFormat",
        ":app:test",
        ":app:ktlintCheck",
        ":app:detekt",
    )
}
