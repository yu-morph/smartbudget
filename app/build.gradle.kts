import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

abstract class GenerateBuildMetadataTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val buildDateTime =
            ZonedDateTime
                .now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 a h:mm", Locale.KOREAN))
        val outputFile =
            outputDirectory
                .file("com/yu_morph/smartbudget/ui/main/BuildMetadata.kt")
                .get()
                .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package com.yu_morph.smartbudget.ui.main

            internal object BuildMetadata {
                const val BUILD_DATETIME = "$buildDateTime"
            }
            """.trimIndent(),
        )
    }
}

abstract class GenerateOpenSourceLicensesTask : DefaultTask() {
    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile = outputDirectory.file("open_source_licenses.html").get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(sourceFile.get().asFile.readText())
    }
}

val appVersionName = providers.gradleProperty("appVersionName").orElse("1.0")
val appVersionCode = providers.gradleProperty("appVersionCode").map(String::toInt).orElse(1)
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
val generateBuildMetadata = tasks.register<GenerateBuildMetadataTask>("generateBuildMetadata") {
    outputDirectory.set(layout.buildDirectory.dir("generated/source/buildMetadata"))
    outputs.upToDateWhen { false }
}

val generateOpenSourceLicenses =
    tasks.register<GenerateOpenSourceLicensesTask>("generateOpenSourceLicenses") {
        sourceFile.set(layout.projectDirectory.file("src/main/licenses/open_source_licenses.html"))
        outputDirectory.set(layout.buildDirectory.dir("generated/assets/openSourceLicenses"))
    }

ktlint {
    android.set(true)
    version.set("1.8.0")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

tasks.matching {
    it.name == "preBuild" ||
        it.name == "detekt" ||
        it.name.startsWith("runKtlintCheck")
}.configureEach {
    mustRunAfter("ktlintFormat")
}

android {
    namespace = "com.yu_morph.smartbudget"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.yu_morph.smartbudget"
        minSdk = 29
        targetSdk = 37
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()

        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath.isPresent) {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }
    buildTypes {
        release {
            if (releaseKeystorePath.isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.java?.addGeneratedSourceDirectory(generateBuildMetadata) { it.outputDirectory }
        variant.sources.assets?.addGeneratedSourceDirectory(generateOpenSourceLicenses) { it.outputDirectory }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.calendar.compose)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
